package NOP.pipeline.core

import spinal.core._
import spinal.lib._

import NOP._
import NOP.constants.`enum`.{CacheOpType, CacheSelType, TLBOpType}
import NOP.pipeline.decode._
import NOP.pipeline.fetch._
import NOP.pipeline.priviledge._

/** WeBattle retirement, precise-state recovery and architectural side effects.
  *
  * The controller is organized around one ordered eligibility scan.  Lane zero
  * owns precise exceptions and global side effects; later lanes may retire
  * only while every older lane is complete and no earlier control recovery
  * blocks them.
  */
final class WeBattleCommitPlugin(config: MyCPUConfig)
    extends CommitPlugin(config) {
  private val width = config.rob.retireWidth
  private val dispatchWidth = config.decode.decodeWidth

  override def build(pipeline: MyCPUCore): Unit = pipeline plug {
    val rob = pipeline.service(classOf[ROBFIFOPlugin])
    val redirect =
      pipeline.fetchPipeline.service(classOf[ProgramCounterPlugin]).backendJumpInterface

    pipeline.RENAME plug new Area {
      import pipeline.RENAME._

      val decoded = input(pipeline.decodePipeline.signals.DECODE_PACKET)
      val renamed = input(pipeline.decodePipeline.signals.RENAME_RECORDS)
      val robIndexes = insert(pipeline.decodePipeline.signals.ROB_INDEXES)

      for (lane <- 0 until dispatchWidth) {
        val source = decoded(lane)
        val target = rob.fifoIO.push(lane)

        target.payload.info.uop.assignSomeByName(source.payload)
        target.payload.info.rename.assignAllByName(renamed(lane))
        target.payload.info.frontendExc := source.payload.except.valid
        target.payload.state.complete := source.payload.needNotExecute
        target.payload.state.except.assignSomeByName(source.payload.except)
        target.payload.state.mispredict :=
          !source.payload.branchLike && source.payload.predInfo.predictTaken
        target.payload.state.actualTaken := False

        target.valid := arbitration.isValidNotStuck && source.valid
        arbitration.haltItself setWhen (
          arbitration.isValid && source.valid && !target.ready
        )
        robIndexes(lane) := rob.pushPtr + lane
      }
    }

    val retirement = new Area {
      val ports = rob.fifoIO.pop
      val head = ports(0)
      val headUop = head.payload.info.uop
      val headState = head.payload.state

      arfCommits.foreach(_.setIdle())
      predUpdate.setIdle()
      ertn := False
      epc := headUop.pc
      CSRWrite.setIdle()
      cacheOp.setIdle()
      tlbOp := TLBOpType.NONE
      tlbInvASID := 0
      tlbInvVPPN := 0
      doWait := False
      commitStore := False

      val uncachedExecuting = RegInit(False)
      val inhibitInterrupt = Bool()
      inhibitInterrupt := uncachedExecuting
      val interruptPending =
        pipeline.service(classOf[InterruptHandlerPlugin]).intPending
      val headHasException =
        headState.except.valid || (interruptPending && !inhibitInterrupt)
      val linearRecovery =
        headUop.flushState ||
          (!headUop.branchLike && headState.mispredict)

      val uncachedPermit = Bits(width bits)
      uncachedPermit.setAll()
      val headIsUncachedLoad =
        headUop.isLoad && headState.lsuUncached
      val memoryWriteback = pipeline.memPipeline.WB
      val memorySlot =
        memoryWriteback.input(pipeline.memPipeline.signals.STD_SLOT)
      val uncachedResponse =
        memoryWriteback.arbitration.notStuck &&
          memorySlot.valid && !memorySlot.isStore

      when(!uncachedExecuting) {
        when(
          head.valid && headState.complete && headIsUncachedLoad &&
            !headHasException
        ) {
          uncachedPermit := 0
          commitStore := True
          uncachedExecuting := True
        }
      } otherwise {
        uncachedPermit := 0
        when(uncachedResponse) {
          uncachedPermit.setAll()
          uncachedExecuting := False
        }
      }
      DuncachedMask := uncachedPermit

      val completePrefix = Bits(width bits)
      val exceptionBarrier = Bits(width bits)
      val uniqueBarrier = Bits(width bits)
      val recoveryBarrier = Bits(width bits)

      for (lane <- 0 until width) {
        completePrefix(lane) :=
          ports.take(lane + 1).map(_.payload.state.complete).andR

        if (lane == 0) {
          exceptionBarrier(lane) := True
          uniqueBarrier(lane) := True
          recoveryBarrier(lane) := True
        } else {
          exceptionBarrier(lane) :=
            !linearRecovery &&
              !ports.take(lane + 1)
                .map(_.payload.state.except.valid)
                .orR
          uniqueBarrier(lane) :=
            !ports.slice(1, lane + 1)
              .map(_.payload.info.uop.uniqueRetire)
              .orR
          recoveryBarrier(lane) :=
            !ports.take(lane)
              .map(_.payload.state.mispredict)
              .orR
        }
      }

      val retireMask =
        completePrefix & exceptionBarrier & uniqueBarrier &
          recoveryBarrier & uncachedPermit
      for (lane <- 0 until width) {
        ports(lane).ready := retireMask(lane)
      }

      val headFire = head.fire
      val recoverNow =
        headFire &&
          (headHasException || headState.mispredict || linearRecovery)
      needFlush := recoverNow
      rob.fifoIO.flush := needFlush
      recoverPRF := regFlush

      pipeline.fetchPipeline.stages.last.arbitration.flushIt setWhen needFlush
      pipeline.decodePipeline.stages.last.arbitration.flushIt setWhen needFlush

      except.valid := headFire && headHasException
      except.payload := headState.except.payload
      when(head.payload.info.frontendExc) {
        except.badVA := headUop.pc
      }

      when(headFire && !headHasException) {
        cacheOp.valid := headUop.operateCache
        cacheOp.payload.addr := headState.except.badVA
        cacheOp.payload.op.assignFromBits(
          headState.intResult(0, CacheOpType.None.asBits.getWidth bits).asBits
        )
        cacheOp.payload.sel.assignFromBits(
          headState.intResult(
            CacheOpType.None.asBits.getWidth,
            CacheSelType.None.asBits.getWidth bits
          ).asBits
        )

        doWait := headUop.isWait
        tlbOp := headUop.tlbOp
        tlbInvASID := headState.intResult(9 downto 0).asBits
        tlbInvVPPN := headState.intResult(28 downto 10).asBits
        ertn := headUop.isErtn
        commitStore := headUop.isStore

        when(headUop.writeCSR) {
          CSRWrite.valid := True
          CSRWrite.payload.data := headState.intResult.asBits
          CSRWrite.payload.addr := headUop.inst(23 downto 10).asUInt
        }

        val exceptionState = pipeline.service(classOf[ExceptionHandlerPlugin])
        when(headUop.isLL) {
          exceptionState.LLBCTL_LLBIT := True
        }
        when(headUop.isSC) {
          when(exceptionState.LLBCTL_LLBIT) {
            exceptionState.LLBCTL_LLBIT := False
          } otherwise {
            commitStore := False
          }
        }

        predUpdate.valid :=
          headUop.branchLike || headUop.predInfo.predictBranch
        predUpdate.payload.predInfo := headUop.predInfo
        predUpdate.payload.predRecover := headUop.predRecover
        predUpdate.payload.branchLike :=
          headUop.isBranch || headUop.isJump || headUop.isJR
        predUpdate.payload.isTaken :=
          (headState.mispredict ^ headUop.predInfo.predictTaken) ||
            headUop.isJump || headUop.isJR
        predUpdate.payload.isRet :=
          headUop.inst === B(0x4c000020L, 32 bits)
        predUpdate.payload.isCall :=
          (headUop.isJR && headUop.inst(4 downto 0) === B"00001") ||
            (headUop.isJump && headUop.inst(26))
        predUpdate.payload.mispredict := headState.mispredict
        predUpdate.payload.pc := headUop.pc
        predUpdate.payload.target := headState.intResult

        when(linearRecovery && !headUop.isErtn) {
          redirect.valid := True
          redirect.payload := headUop.pc + 4
        }
        when(headState.mispredict && headUop.branchLike) {
          redirect.valid := True
          when(headUop.isJump || headUop.isJR) {
            redirect.payload := headState.intResult
          } otherwise {
            redirect.payload := Mux(
              headState.actualTaken,
              headState.intResult,
              headUop.pc + 4
            )
          }
        }
      }

      for (lane <- 0 until width) {
        val port = ports(lane)
        val uop = port.payload.info.uop
        val mapping = port.payload.info.rename
        when(port.fire && !headHasException) {
          arfCommits(lane).valid := uop.doRegWrite
          arfCommits(lane).payload.addr := uop.wbAddr
          arfCommits(lane).payload.prevAddr := mapping.wPrevReg
          arfCommits(lane).payload.prfAddr := mapping.wReg
        }
      }
    }

    retirement
  }
}
