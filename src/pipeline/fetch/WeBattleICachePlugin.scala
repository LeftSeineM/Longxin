package NOP.pipeline.fetch

import NOP._
import NOP.constants.`enum`.{CacheOpType, CacheSelType}
import NOP.pipeline.core.{CommitPlugin, ExceptionMuxPlugin}
import NOP.utils._
import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

/** WeBattle-owned instruction-cache controller.
  *
  * The inherited class supplies only the validated RAM/AXI service shell so
  * existing service lookups and generated memory macros remain compatible.
  * Hit selection, miss sequencing, packet construction and invalidate control
  * are implemented here as one explicit controller boundary.
  */
class WeBattleICachePlugin(config: MyCPUConfig) extends ICachePlugin(config) {

  override def build(pipeline: FetchPipeline): Unit = pipeline plug new Area {
    import pipeline._

    missEvent = out(Bool()).setName("icache_miss_event")
    missEvent := False

    val requestReplay = False
    val replayActive = IF1.arbitration.isValid && requestReplay
    val nextFetchPC = pipeline.service(classOf[ProgramCounterPlugin]).nextPC
    val arrayReadEnable = !IF1.arbitration.isStuck || replayActive
    val arrayReadAddress =
      Mux(replayActive, IF1.input(pipeline.signals.PC), nextFetchPC)

    val infoRead = infoRAM.io.read
    infoRead.cmd.valid := arrayReadEnable
    infoRead.cmd.payload := arrayReadAddress(icache.indexRange)

    val dataReads = Vec(dataRAMs.map(_.io.read))
    dataReads.foreach { port =>
      port.cmd.valid := arrayReadEnable
      port.cmd.payload := arrayReadAddress(icache.indexRange)
    }

    IF1 plug new Area {
      import IF1._

      arbitration.haltItself setWhen replayActive
      val fetchPC = input(pipeline.signals.PC)
      insert(ICACHE_VALIDS) := valids(fetchPC(icache.indexRange))
      for (way <- 0 until icache.ways; slot <- 0 until frontend.fetchWidth) {
        insert(ICACHE_RSPS)(way)(slot) :=
          dataRAMs(way).io.read.rsp(fetchPC(icache.wordOffsetRange) + slot)
      }
      insert(ICACHE_INFO) := infoRead.rsp
    }

    IF2 plug new Area {
      import IF2._

      val exceptionMux = pipeline.service(classOf[ExceptionMuxPlugin[FetchPipeline]])
      val hasException = output(exceptionMux.ExceptionSignals.EXCEPTION_OCCURRED)
      val requestValid = arbitration.isValidOnEntry && !hasException
      val requestFire = requestValid && !arbitration.isStuck
      val virtualPC = input(pipeline.signals.PC)
      val physicalPC = input(pipeline.signals.PC_PHYSICAL)
      val wordOffset = virtualPC(icache.wordOffsetRange)
      val setIndex = virtualPC(icache.indexRange)
      val physicalTag = physicalPC(icache.tagRange)

      val infoWrite = infoRAM.io.write.setIdle()
      val dataWrites = Vec(dataRAMs.map(_.io.write.setIdle()))
      val byteMasks =
        Vec(dataRAMs.map(_.io.writeMask.clearAll().subdivideIn(4 bits, true)))

      val wayHits = input(ICACHE_VALIDS)
        .zip(input(ICACHE_INFO).tags)
        .map { case (isValid, storedTag) => isValid && storedTag === physicalTag }
      val cacheHit = wayHits.orR
      val selectedWords = MuxOH(wayHits, input(ICACHE_RSPS))

      val refillPacket = Vec(Reg(BWord()), frontend.fetchWidth)

      when(requestFire && cacheHit) {
        assert(icache.ways == 2)
        val hitInfo = input(ICACHE_INFO).copy()
        hitInfo.tags := input(ICACHE_INFO).tags
        hitInfo.lru(0) := wayHits(0)
        infoWrite.valid := True
        infoWrite.address := setIndex
        infoWrite.data := hitInfo
      }

      val refill = new StateMachine {
        val issueAddress = new State
        val receiveLine = new State
        val installLine = new State
        val releasePipeline = new State

        disableAutoStart()
        setEntry(stateBoot)

        val beat = Counter(icache.lineWords)
        val delayedBeatValid = RegNext(iBus.r.fire)
        val delayedBeatData = RegNext(iBus.r.payload.data)
        val victimWay = input(ICACHE_INFO).lru.asUInt

        for (slot <- 0 until frontend.fetchWidth) {
          when(delayedBeatValid && beat === wordOffset + slot) {
            refillPacket(slot) := delayedBeatData
          }
        }

        stateBoot.whenIsActive {
          when(requestValid && !cacheHit) {
            missEvent := True
            arbitration.haltItself := True
            goto(issueAddress)
          }
        }

        issueAddress.whenIsActive {
          arbitration.haltItself := True
          iBus.ar.valid := True
          iBus.ar.id := 0
          iBus.ar.addr :=
            physicalPC(31 downto icache.offsetWidth) @@ U(0, icache.offsetWidth bits)
          iBus.ar.len := icache.lineWords - 1
          iBus.ar.size := 2
          iBus.ar.burst := 1
          iBus.ar.lock := 0
          iBus.ar.cache := 0
          if (config.axiConfig.useQos) iBus.ar.qos := 0
          iBus.ar.prot := 0
          when(iBus.ar.ready) {
            beat.clear()
            goto(receiveLine)
          }
        }

        receiveLine.whenIsActive {
          arbitration.haltItself := True
          iBus.r.ready := True
          when(delayedBeatValid) {
            dataWrites(victimWay).valid := True
            dataWrites(victimWay).address := setIndex
            dataWrites(victimWay).data.foreach(_ := delayedBeatData)
            byteMasks(victimWay)(beat).setAll()
            beat.increment()
          }
          when(iBus.r.valid && iBus.r.last) {
            goto(installLine)
          }
        }

        installLine.whenIsActive {
          arbitration.haltItself := True

          dataWrites(victimWay).valid := True
          dataWrites(victimWay).address := setIndex
          dataWrites(victimWay).data.foreach(_ := delayedBeatData)
          byteMasks(victimWay)(beat).setAll()

          assert(icache.ways == 2)
          val installedInfo = input(ICACHE_INFO).copy()
          installedInfo.tags := input(ICACHE_INFO).tags
          installedInfo.tags(victimWay) := physicalTag
          installedInfo.lru := ~input(ICACHE_INFO).lru
          valids(setIndex)(victimWay) := True

          infoWrite.valid := True
          infoWrite.address := setIndex
          infoWrite.data := installedInfo
          goto(releasePipeline)
        }

        releasePipeline.whenIsActive {
          when(!arbitration.isStuck) {
            requestReplay := !IF1.arbitration.isFlushed
            goto(stateBoot)
          }
        }
      }

      val packet = insert(pipeline.signals.FETCH_PACKET)
      packet.pc := virtualPC
      packet.except.valid := hasException
      packet.except.payload.code :=
        output(exceptionMux.ExceptionSignals.EXCEPTION_ECODE)
      packet.except.payload.subcode :=
        output(exceptionMux.ExceptionSignals.EXCEPTION_ESUBCODE)
      packet.except.payload.isTLBRefill := output(pipeline.signals.IS_TLB_REFILL)

      for (slot <- 0 until frontend.fetchWidth) {
        if (slot == 0) packet.insts(slot).valid := True
        else packet.insts(slot).valid := wordOffset < icache.lineWords - slot
        packet.insts(slot).payload :=
          Mux(cacheHit, selectedWords(slot), refillPacket(slot))
      }
    }

    val invalidate = new Area {
      val operation = pipeline.globalService(classOf[CommitPlugin]).cacheOp
      val targetICache = operation.valid && operation.payload.sel === CacheSelType.ICache
      val targetWay = operation.addr(0, log2Up(icache.ways) bits)
      val targetSet = operation.addr(icache.indexRange)

      when(targetICache) {
        switch(operation.op) {
          is(CacheOpType.None) {}
          is(CacheOpType.IndexInvalidate) {
            valids(targetSet)(targetWay) := False
          }
          is(CacheOpType.HitInvalidate) {
            valids(targetSet).foreach(_ := False)
          }
          is(CacheOpType.StoreTag) {
            for (set <- 0 until icache.sets; way <- 0 until icache.ways) {
              valids(set)(way) := False
            }
          }
        }
      }
    }
  }
}
