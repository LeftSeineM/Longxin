package NOP.pipeline.fetch

import NOP._
import NOP.utils._
import spinal.core._
import spinal.lib._

/** Independently maintained speculative return-address stack.
  *
  * `nextFree` uses a different state convention from the inherited RAS: it
  * points to the slot that the next call will fill.  Every fetched instruction
  * receives this pointer as its checkpoint.  A committed branch correction can
  * therefore restore the speculative state and atomically apply the real
  * call/return effect.
  *
  * Recovery has explicit priority over a same-cycle speculative update.  This
  * is important because a redirect invalidates the younger fetch operation.
  */
class WeBattleCheckpointRAS(config: FrontendConfig) extends Component {
  private val entries = config.btb.rasEntries
  private val pointerWidth = log2Up(entries)

  require(isPow2(entries))

  val io = new Bundle {
    val checkpoint = out(UInt(pointerWidth bits))

    val speculativeFire = in(Bool())
    val speculativeCall = in(Bool())
    val speculativeReturn = in(Bool())
    val speculativeReturnWord = in(UInt(30 bits))

    val prediction = master(Flow(UWord()))

    val repairValid = in(Bool())
    val repairMispredict = in(Bool())
    val repairCall = in(Bool())
    val repairReturn = in(Bool())
    val repairCheckpoint = in(UInt(pointerWidth bits))
    val repairPcWord = in(UInt(30 bits))
  }

  val returnWords = Vec(RegInit(U(config.pcInit >> 2, 30 bits)), entries)
  val nextFree = RegInit(U(0, pointerWidth bits))

  io.checkpoint := nextFree
  io.prediction.setIdle()

  // A return reads the entry immediately below the next-free pointer.  All
  // entries have a deterministic reset value, including an unmatched return.
  val speculativeReturnIndex = nextFree - 1
  when(io.speculativeFire && !io.speculativeCall && io.speculativeReturn) {
    io.prediction.push(returnWords(speculativeReturnIndex) @@ U(0, 2 bits))
  }

  when(io.repairValid) {
    when(io.repairMispredict) {
      when(io.repairCall) {
        returnWords(io.repairCheckpoint) := io.repairPcWord + 1
        nextFree := io.repairCheckpoint + 1
      } elsewhen (io.repairReturn) {
        nextFree := io.repairCheckpoint - 1
      } otherwise {
        nextFree := io.repairCheckpoint
      }
    } otherwise {
      // The predictor classified an instruction as a branch but commit proved
      // that it is not one.  Discard all younger speculative RAS activity.
      nextFree := io.repairCheckpoint
    }
  } elsewhen (io.speculativeFire) {
    when(io.speculativeCall) {
      returnWords(nextFree) := io.speculativeReturnWord
      nextFree := nextFree + 1
    } elsewhen (io.speculativeReturn) {
      nextFree := nextFree - 1
    }
  }
}
