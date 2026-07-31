package NOP.pipeline.mem

import spinal.core._
import spinal.lib._

import NOP._
import NOP.builder._
import NOP.constants.enum._
import NOP.pipeline._
import NOP.pipeline.core._
import NOP.pipeline.exe.MemIssueQueuePlugin
import NOP.pipeline.priviledge.ExceptionHandlerPlugin
import NOP.utils._

/** WeBattle-owned LSU issue, register-read, wakeup and completion controller.
  *
  * The cache datapath remains a separate service. This class owns the pipeline
  * control contract on both sides of it and deliberately preserves the N12
  * stage count and speculative-wakeup behavior.
  */
final class WeBattleMemExecutePlugin(config: MyCPUConfig)
    extends MemExecutePlugin(config) {
  private val operandPorts = config.regFile.rPortsEachInst

  override def build(pipeline: MemPipeline): Unit = {
    val exceptionSignals =
      pipeline
        .service(classOf[ExceptionMuxPlugin[pipeline.type]])
        .ExceptionSignals
    val commit = pipeline.globalService(classOf[CommitPlugin])
    val flush = commit.regFlush

    import pipeline.signals._

    pipeline plug new Area {
      // A recovery atomically removes every in-flight memory operation after
      // the issue stage. The store buffer separately preserves retired stores.
      pipeline.stages.drop(1).foreach { stage =>
        stage.arbitration.removeIt.setWhen(flush)
      }
    }

    pipeline.ISS plug new Area {
      import pipeline.ISS._

      val issueQueue =
        pipeline.globalService(classOf[MemIssueQueuePlugin])
      val storeBuffer = pipeline.service(classOf[StoreBufferPlugin])
      val head = issueQueue.queue(0)

      require(operandPorts == 2)
      require(storeBuffer.depth >= 5)

      val operandsReady =
        (0 until operandPorts).map(head.payload.rRegs(_).valid).andR
      // RRD, MEM1 and MEM2 can all become store-buffer producers. Preserve
      // five slots of headroom so an already-issued operation never deadlocks.
      val reserveExhausted =
        storeBuffer.queue(storeBuffer.depth - 5).valid
      val request =
        head.valid && operandsReady && !reserveExhausted

      issueQueue.issueReq := request
      issueQueue.issueFire.clearWhen(arbitration.isStuck)

      insert(ISSUE_SLOT) := head.payload

      val issuingStore = request && head.payload.uop.isStore
      val buffered = storeBuffer.queueIO.popPort
      val compatibleStoreAddress =
        issuingStore && !head.payload.uop.isSC
      val drainBuffered =
        buffered.valid &&
          buffered.payload.retired &&
          (compatibleStoreAddress || !request)

      buffered.ready.setWhen(!arbitration.isStuck && drainBuffered)
      insert(STD_SLOT).valid := drainBuffered
      insert(STD_SLOT).payload := buffered.payload

      arbitration.removeIt.setWhen(!arbitration.isStuck && !request)
      arbitration.removeIt.setWhen(flush && !arbitration.isStuck)
    }

    pipeline.RRD plug new Area {
      import pipeline.RRD._

      val issued = input(ISSUE_SLOT)
      for (operand <- 0 until operandPorts) {
        rrdReq(operand) := issued.rRegs(operand).payload
        insert(REG_READ_RSP)(operand) := rrdRsp(operand)
      }

      val signedOffset =
        issued.uop.immField.asSInt.resize(32 bits).asUInt
      insert(MEMORY_ADDRESS) :=
        input(REG_READ_RSP)(0).asUInt + signedOffset
      insert(MEMORY_WRITE_DATA) := input(REG_READ_RSP)(1)
    }

    pipeline.MEM1 plug new Area {
      import pipeline.MEM1._

      val issued = input(ISSUE_SLOT)
      val buffered = input(STD_SLOT)
      val bufferedLoad = buffered.valid && !buffered.isStore
      val destinationValid = Mux(
        buffered.valid,
        buffered.payload.wReg.valid,
        issued.uop.doRegWrite
      )
      val destination = Mux(
        buffered.valid,
        buffered.payload.wReg.payload,
        issued.wReg
      )
      val predictableResult =
        (arbitration.isValid &&
          (input(ADDRESS_CACHED) || issued.uop.isSC)) ||
          bufferedLoad

      clrBusy.valid :=
        predictableResult &&
          arbitration.notStuck &&
          destinationValid
      clrBusy.payload := destination
    }

    pipeline.MEM2 plug new Area {
      import pipeline.MEM2._

      val buffered = input(STD_SLOT)
      val bufferedLoad = buffered.valid && !buffered.isStore

      pipeline
        .globalService(classOf[SpeculativeWakeupHandler])
        .wakeupFailed
        .setWhen(arbitration.isStuck && output(WRITE_REG).valid)

      when(!input(exceptionSignals.EXCEPTION_OCCURRED)) {
        // CACHE operations reuse badVA to transport the physical address.
        output(MEMORY_ADDRESS) := input(MEMORY_ADDRESS_PHYSICAL)
      }
      when(bufferedLoad) {
        output(WRITE_REG) := buffered.wReg
      }
    }

    pipeline.WB plug new Area {
      import pipeline.WB._

      val issued = input(ISSUE_SLOT)
      val buffered = input(STD_SLOT)
      val bufferedLoad = buffered.valid && !buffered.isStore
      val resultAvailable =
        (arbitration.isValid &&
          (input(ADDRESS_CACHED) || issued.uop.isSC)) ||
          bufferedLoad

      wPort.valid :=
        resultAvailable &&
          arbitration.notStuck &&
          input(WRITE_REG).valid
      wPort.addr := input(WRITE_REG).payload
      wPort.data := input(MEMORY_READ_DATA)

      when(arbitration.isValid && issued.uop.isSC) {
        val exceptionState =
          pipeline.globalService(classOf[ExceptionHandlerPlugin])
        wPort.addr := issued.wReg
        wPort.data :=
          exceptionState.LLBCTL_LLBIT.asUInt.resize(32 bits).asBits
      }

      robWrite.valid := arbitration.isValidNotStuck
      robWrite.robIdx := input(ROB_IDX)
      robWrite.except.valid :=
        robWrite.valid && input(exceptionSignals.EXCEPTION_OCCURRED)
      robWrite.except.payload.code :=
        input(exceptionSignals.EXCEPTION_ECODE)
      robWrite.except.payload.subcode :=
        input(exceptionSignals.EXCEPTION_ESUBCODE)
      robWrite.except.payload.badVA := input(MEMORY_ADDRESS)
      robWrite.except.payload.isTLBRefill := input(IS_TLB_REFILL)
      robWrite.lsuUncached := !input(ADDRESS_CACHED)
      robWrite.intResult := wPort.data.asUInt

      when(issued.uop.cacheOp =/= CacheOpType.None) {
        val opWidth = CacheOpType.None.asBits.getWidth
        val selectWidth = CacheSelType.None.asBits.getWidth
        robWrite.intResult(0, opWidth bits) :=
          issued.uop.cacheOp.asBits.asUInt
        robWrite.intResult(opWidth, selectWidth bits) :=
          issued.uop.cacheSel.asBits.asUInt
      }

      robWrite.isLoad := input(IS_LOAD) && robWrite.valid
      robWrite.isStore := input(IS_STORE) && robWrite.valid
      robWrite.isLL := issued.uop.isLL && robWrite.valid
      robWrite.isSC := issued.uop.isSC && robWrite.valid
      robWrite.lsType := issued.uop.lsType
      robWrite.vAddr := input(MEMORY_ADDRESS)
      robWrite.pAddr := input(MEMORY_ADDRESS_PHYSICAL)
      robWrite.storeData := input(MEMORY_WRITE_DATA).asUInt
      robWrite.myPC := issued.uop.pc
    }

    // STD validity is transactional: a stalled stage must not let a consumer
    // observe the same buffered operation twice.
    pipeline.stages.foreach { stage =>
      stage.output(STD_SLOT).valid.clearWhen(stage.arbitration.isStuck)
    }

    Component.current.afterElaboration {
      pipeline.stages.drop(1).foreach { stage =>
        stage.input(STD_SLOT).valid.getDrivingReg().init(False)
      }
    }
  }
}
