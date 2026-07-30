package NOP.pipeline.fetch

import NOP.pipeline._
import NOP.builder._
import NOP.utils._
import NOP._
import NOP.pipeline.core.CommitPlugin
import spinal.core._
import spinal.lib._

import scala.collection.mutable.ArrayBuffer

class ReturnAddressStackPlugin(config: FrontendConfig) extends Plugin[FetchPipeline] {
  require(isPow2(config.btb.rasEntries))
  val rasPredict = Flow(UWord()).setIdle()

  override def build(pipeline: FetchPipeline): Unit = {
    if (config.useWeBattleCheckpointRAS) {
      buildWeBattle(pipeline)
    } else {
      buildInherited(pipeline)
    }
  }

  private def buildWeBattle(pipeline: FetchPipeline): Unit = {
    val checkpointRAS = new WeBattleCheckpointRAS(config)

    pipeline.IF2 plug new Area {
      import pipeline.IF2._
      import pipeline.signals._

      val jumpFlag = input(PREDICT_JUMP_FLAG)
      val jumpPayload = input(PREDICT_JUMP_PAYLOAD)
      val jumpWay = input(PREDICT_JUMP_WAY)

      insert(RECOVER_TOP) := checkpointRAS.io.checkpoint
      checkpointRAS.io.speculativeFire :=
        arbitration.isValid && !arbitration.isStuck && jumpFlag
      checkpointRAS.io.speculativeCall := jumpPayload.isCall
      checkpointRAS.io.speculativeReturn := jumpPayload.isReturn
      checkpointRAS.io.speculativeReturnWord :=
        input(PC)(2, 30 bits) + jumpWay + 1

      when(checkpointRAS.io.prediction.valid) {
        rasPredict.push(checkpointRAS.io.prediction.payload)
      }
    }

    pipeline plug new Area {
      val bpuCommit = pipeline.globalService(classOf[CommitPlugin])
      val predUpdate = bpuCommit.predUpdate
      val payload = predUpdate.payload

      checkpointRAS.io.repairValid :=
        predUpdate.valid &&
          ((payload.predInfo.predictBranch && !payload.branchLike) ||
            payload.mispredict)
      checkpointRAS.io.repairMispredict := payload.mispredict
      checkpointRAS.io.repairCall := payload.isCall
      checkpointRAS.io.repairReturn := payload.isRet
      checkpointRAS.io.repairCheckpoint := payload.predRecover.recoverTop
      checkpointRAS.io.repairPcWord := payload.pc(31 downto 2)
    }
  }

  /** Exact fallback used by N0--N6 for reproducible A/B comparison. */
  private def buildInherited(pipeline: FetchPipeline): Unit = {
    val ras = Vec(RegInit(U(config.pcInit >> 2, 30 bits)), config.btb.rasEntries)
    val rasTopEntry = RegInit(U(0, log2Up(config.btb.rasEntries) bits))

    pipeline.IF2 plug new Area {
      import pipeline.IF2._
      import pipeline.signals._

      val jumpFlag = input(PREDICT_JUMP_FLAG)
      val jumpPayload = input(PREDICT_JUMP_PAYLOAD)
      val jumpWay = input(PREDICT_JUMP_WAY)

      insert(RECOVER_TOP) := rasTopEntry

      when(arbitration.isValid && !arbitration.isStuck && jumpFlag) {
        when(jumpPayload.isCall) {
          ras(rasTopEntry + 1) := input(PC)(2, 30 bits) + jumpWay + 1
          rasTopEntry := rasTopEntry + 1
        } elsewhen (jumpPayload.isReturn) {
          rasTopEntry := rasTopEntry - 1
          rasPredict.push(ras(rasTopEntry) @@ U(0, 2 bits))
        }
      }
    }

    pipeline plug new Area {
      val bpuCommit = pipeline.globalService(classOf[CommitPlugin])
      val predUpdate = bpuCommit.predUpdate

      when(predUpdate.valid) {
        val payload = predUpdate.payload
        val pred = payload.predInfo
        val recover = payload.predRecover
        when(pred.predictBranch && !payload.branchLike) {
          rasTopEntry := recover.recoverTop
        }

        when(payload.mispredict) {
          when(payload.isCall) {
            rasTopEntry := recover.recoverTop + 1
            ras(recover.recoverTop + 1) := payload.pc(31 downto 2) + 1
          } elsewhen (payload.isRet) {
            rasTopEntry := recover.recoverTop - 1
          } otherwise {
            rasTopEntry := recover.recoverTop
          }
        }
      }
    }
  }
}
