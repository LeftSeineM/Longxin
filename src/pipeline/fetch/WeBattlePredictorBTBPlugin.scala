package NOP.pipeline.fetch

import NOP._
import NOP.builder._
import NOP.pipeline._
import NOP.pipeline.core.CommitPlugin
import NOP.utils._
import spinal.core._
import spinal.lib._

/** WeBattle-owned direction predictor.
  *
  * The global table and the packed bimodal/chooser table are deliberately
  * kept behind one explicit read/update contract. This makes speculative
  * history movement independent of table writes and keeps commit recovery
  * atomic.
  */
class WeBattleDirectionPredictor(config: FrontendConfig, fetchWidth: Int)
    extends Component {
  private val counterWidth = config.bpu.counterWidth
  private val counterType = HardType(UInt(counterWidth bits))
  private val packedType = HardType(UInt((counterWidth * 2) bits))
  private val historyType = HardType(UInt(config.bpu.historyWidth bits))
  private val packedSets = config.bpu.phtSets / 2

  require(!config.bpu.useTournament || packedSets >= fetchWidth)

  val io = new Bundle {
    val read = new Bundle {
      val enable = in(Bool())
      val nextPC = in(UWord())
      val globalHistory = out(historyType())
      val globalCounters = out(Vec(counterType, fetchWidth))
      val bimodalCounters = out(Vec(counterType, fetchWidth))
      val chooserCounters = out(Vec(counterType, fetchWidth))
      val selectedCounters = out(Vec(counterType, fetchWidth))
      val taken = out(Bits(fetchWidth bits))
    }
    val historyMove = in(Flow(historyType))
    val commit = in(Flow(new Bundle {
      val pc = UWord()
      val history = historyType()
      val globalCounter = counterType()
      val bimodalCounter = counterType()
      val chooserCounter = counterType()
    }))
  }

  private val history = Reg(historyType) init 0
  private val globalTable = new ReorderCacheRAM(
    counterType,
    config.bpu.phtSets,
    fetchWidth,
    false
  )
  private val packedTournament =
    if (config.bpu.useTournament)
      Some(new ReorderCacheRAM(packedType, packedSets, fetchWidth, false))
    else None

  private val globalInit = Seq.fill(globalTable.wordsPerBank)(config.bpu.initialCounter)
  globalTable.rams.foreach(_.memGeneric.MEMORY_INIT_PARAM = globalInit.mkString(","))

  private val packedInitWord =
    (config.bpu.initialCounter << counterWidth) | config.bpu.initialCounter
  packedTournament.foreach { table =>
    val init = Seq.fill(table.wordsPerBank)(packedInitWord).mkString(",")
    table.rams.foreach(_.memGeneric.MEMORY_INIT_PARAM = init)
  }

  private def globalIndex(pc: UInt, branchHistory: UInt): UInt = {
    if (config.bpu.useBankSafeGShare) {
      val bankBits = log2Up(fetchWidth)
      val pcBits = pc(2 + config.bpu.phtIndexWidth - 1 downto 2)
      val mixedHistory = UInt(config.bpu.phtIndexWidth bits)
      mixedHistory := 0
      mixedHistory(bankBits + config.bpu.historyWidth - 1 downto bankBits) :=
        branchHistory
      pcBits ^ mixedHistory
    } else {
      branchHistory @@ pc(config.bpu.phtPCRange)
    }
  }

  private def pcIndex(pc: UInt): UInt =
    pc(2 + log2Up(packedSets) - 1 downto 2)

  val effectiveHistory = historyType()
  effectiveHistory := Mux(io.historyMove.valid, io.historyMove.payload, history)
  when(io.historyMove.valid) {
    history := io.historyMove.payload
  }
  io.read.globalHistory := history

  globalTable.io.read.cmd.valid := io.read.enable
  globalTable.io.read.cmd.payload := globalIndex(io.read.nextPC, effectiveHistory)
  io.read.globalCounters := globalTable.io.read.rsp

  if (config.bpu.useTournament) {
    val table = packedTournament.get
    table.io.read.cmd.valid := io.read.enable
    table.io.read.cmd.payload := pcIndex(io.read.nextPC)
    for (slot <- 0 until fetchWidth) {
      io.read.bimodalCounters(slot) :=
        table.io.read.rsp(slot)(counterWidth - 1 downto 0)
      io.read.chooserCounters(slot) :=
        table.io.read.rsp(slot)(counterWidth * 2 - 1 downto counterWidth)
    }
  } else {
    io.read.bimodalCounters := globalTable.io.read.rsp
    io.read.chooserCounters.foreach(_ := config.bpu.initialCounter)
  }

  for (slot <- 0 until fetchWidth) {
    io.read.selectedCounters(slot) := Mux(
      io.read.chooserCounters(slot).msb,
      io.read.globalCounters(slot),
      io.read.bimodalCounters(slot)
    )
    io.read.taken(slot) := io.read.selectedCounters(slot).msb
  }

  globalTable.io.write.valid := io.commit.valid
  globalTable.io.write.address := globalIndex(io.commit.pc, io.commit.history)
  globalTable.io.write.mask := B(1, fetchWidth bits)
  globalTable.io.write.data.assignDontCare()
  globalTable.io.write.data(0) := io.commit.globalCounter

  packedTournament.foreach { table =>
    table.io.write.valid := io.commit.valid
    table.io.write.address := pcIndex(io.commit.pc)
    table.io.write.mask := B(1, fetchWidth bits)
    table.io.write.data.assignDontCare()
    table.io.write.data(0) := io.commit.chooserCounter @@ io.commit.bimodalCounter
  }
}

/** Complete WeBattle BTB, direction-prediction and history-control boundary. */
class WeBattlePredictorBTBPlugin(config: FrontendConfig)
    extends Plugin[FetchPipeline] {
  private val fetchWidth = config.fetchWidth
  private val btbConfig = config.btb
  private val validBits = Vec(RegInit(False), btbConfig.sets)
  private val targetTable =
    new ReorderCacheRAM(BranchTableEntry(btbConfig), btbConfig.sets, fetchWidth, false)
  private val direction = new WeBattleDirectionPredictor(config, fetchWidth)

  private val predictedTarget = Flow(UWord())

  override def setup(pipeline: FetchPipeline): Unit = {
    pipeline.service(classOf[ProgramCounterPlugin]).setPredict(predictedTarget)
  }

  override def build(pipeline: FetchPipeline): Unit = {
    import pipeline.signals._

    pipeline plug new Area {
      val nextPC = pipeline.service(classOf[ProgramCounterPlugin]).nextPC
      targetTable.io.read.cmd.valid := pipeline.IF1.arbitration.notStuck
      targetTable.io.read.cmd.payload := nextPC(btbConfig.indexRange)
      direction.io.read.enable := pipeline.IF1.arbitration.notStuck
      direction.io.read.nextPC := nextPC
    }

    pipeline.IF1 plug new Area {
      import pipeline.IF1._

      val fetchPC = input(PC)
      val tag = fetchPC(btbConfig.tagRange)
      val baseIndex = fetchPC(btbConfig.indexRange)
      val fetchOffset = fetchPC(config.icache.wordOffsetRange)
      val entries = targetTable.io.read.rsp

      insert(PREDICT_JUMP_FLAG) := False
      insert(PREDICT_JUMP_PAYLOAD) := entries(0).statusBundle
      insert(PREDICT_JUMP_WAY) := 0
      insert(INSTRUCTION_MASK).setAll()
      insert(BRANCH_MASK).clearAll()

      for (slot <- fetchWidth - 1 downto 0) {
        insert(PRED_COUNTER)(slot) := 0
        insert(BIMODAL_COUNTER)(slot) := 0
        insert(CHOOSER_COUNTER)(slot) := config.bpu.initialCounter

        val slotIndex = baseIndex + slot
        val inLine = (fetchOffset + slot) >= fetchOffset
        val entry = entries(slot)
        val entryHit = validBits(slotIndex) && entry.tag === tag

        when(inLine && entryHit) {
          insert(PRED_COUNTER)(slot) := direction.io.read.globalCounters(slot)
          insert(BIMODAL_COUNTER)(slot) := direction.io.read.bimodalCounters(slot)
          insert(CHOOSER_COUNTER)(slot) := direction.io.read.chooserCounters(slot)
          insert(BRANCH_MASK)(slot) := True

          when(direction.io.read.taken(slot)) {
            insert(PREDICT_JUMP_FLAG) := True
            insert(PREDICT_JUMP_PAYLOAD) := entry.statusBundle
            insert(PREDICT_JUMP_WAY) := slot
            if (slot != fetchWidth - 1) {
              insert(INSTRUCTION_MASK) := (1 << (slot + 1)) - 1
            }
          }
        }
      }

      insert(GLOBAL_BRANCH_HISTORY) := direction.io.read.globalHistory
    }

    pipeline.IF2 plug new Area {
      import pipeline.IF2._

      val selectedTarget = UInt(32 bits)
      selectedTarget := input(PREDICT_JUMP_PAYLOAD).target @@ U(0, 2 bits)
      val ras = pipeline.service(classOf[ReturnAddressStackPlugin]).rasPredict
      when(ras.valid) {
        selectedTarget := ras.payload
      }

      insert(PREDICT_ADDR) := selectedTarget
      predictedTarget.payload := selectedTarget
      predictedTarget.valid := False
      insert(TAKEN_MASK).clearAll()

      when(arbitration.isValid && input(PREDICT_JUMP_FLAG)) {
        insert(TAKEN_MASK)(input(PREDICT_JUMP_WAY)) := True
        predictedTarget.valid := True
        arbitration.flushNext := True
      }

      val branchCount = CountOne(input(INSTRUCTION_MASK) & input(BRANCH_MASK))
      val speculativeHistory = input(GLOBAL_BRANCH_HISTORY) |<< branchCount
      direction.io.historyMove.setIdle()
      when(arbitration.isFiring) {
        direction.io.historyMove.push(
          speculativeHistory | input(PREDICT_JUMP_FLAG).asUInt.resized
        )
      }
      insert(PRIVATE_BRANCH_HISTORY).foreach(_ := input(GLOBAL_BRANCH_HISTORY))
    }

    pipeline plug new Area {
      val predictionUpdate = pipeline.globalService(classOf[CommitPlugin]).predUpdate
      targetTable.io.write.setIdle()
      direction.io.commit.setIdle()

      def saturating(counter: UInt, taken: Bool): UInt = {
        val result = UInt(config.bpu.counterWidth bits)
        result := counter
        when(taken && counter =/= counter.maxValue) {
          result := counter + 1
        } elsewhen (!taken && counter =/= 0) {
          result := counter - 1
        }
        result
      }

      when(predictionUpdate.valid) {
        val update = predictionUpdate.payload
        val previous = update.predInfo
        val recover = update.predRecover
        val entryIndex = update.pc(btbConfig.indexRange)

        targetTable.io.write.address := entryIndex
        targetTable.io.write.mask := B(1, fetchWidth bits)
        direction.io.commit.pc := update.pc
        direction.io.commit.history := recover.ghr

        when(previous.predictBranch && !update.branchLike) {
          validBits(entryIndex) := False
        } elsewhen (!previous.predictBranch && update.branchLike) {
          val fresh = BranchTableEntry(btbConfig)
          fresh.tag := update.pc(btbConfig.tagRange)
          fresh.statusBundle.target := update.target(31 downto 2)
          fresh.statusBundle.isCall := update.isCall
          fresh.statusBundle.isReturn := update.isRet

          val strongTaken = U((1 << config.bpu.counterWidth) - 1,
            config.bpu.counterWidth bits)
          val initial = Mux(
            update.isCall || update.isRet,
            strongTaken,
            Mux(update.isTaken, U(2, config.bpu.counterWidth bits),
              U(1, config.bpu.counterWidth bits))
          )
          direction.io.commit.globalCounter := initial
          direction.io.commit.bimodalCounter := initial
          direction.io.commit.chooserCounter := config.bpu.initialCounter
          targetTable.io.write.data(0) := fresh
          targetTable.io.write.valid := True
          direction.io.commit.valid := True
          validBits(entryIndex) := True
        } elsewhen (previous.predictBranch && update.branchLike) {
          val refreshed = BranchTableEntry(btbConfig)
          refreshed.tag := update.pc(btbConfig.tagRange)
          refreshed.statusBundle.target := update.target(31 downto 2)
          refreshed.statusBundle.isCall := update.isCall
          refreshed.statusBundle.isReturn := update.isRet

          direction.io.commit.globalCounter :=
            saturating(recover.predictCounter, update.isTaken)
          direction.io.commit.bimodalCounter :=
            saturating(recover.bimodalCounter, update.isTaken)
          direction.io.commit.chooserCounter := recover.chooserCounter

          val globalCorrect = recover.predictCounter.msb === update.isTaken
          val bimodalCorrect = recover.bimodalCounter.msb === update.isTaken
          when(globalCorrect && !bimodalCorrect && recover.chooserCounter =/= 3) {
            direction.io.commit.chooserCounter := recover.chooserCounter + 1
          } elsewhen (!globalCorrect && bimodalCorrect && recover.chooserCounter =/= 0) {
            direction.io.commit.chooserCounter := recover.chooserCounter - 1
          }

          targetTable.io.write.data(0) := refreshed
          targetTable.io.write.valid := True
          direction.io.commit.valid := True
        }

        when(update.mispredict) {
          direction.io.historyMove.push((recover.ghr @@ update.isTaken).resized)
        }
      }
    }
  }
}
