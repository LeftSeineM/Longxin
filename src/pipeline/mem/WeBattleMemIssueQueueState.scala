package NOP.pipeline.mem

import spinal.core._
import spinal.lib._

import NOP.pipeline.IssueSlot

/** Timing-oriented state owner for the in-order memory issue FIFO.
  *
  * Unlike the integer reservation station, the memory queue only issues its
  * oldest entry.  Its state transition therefore needs neither a population
  * count nor a multi-removal rank network.  New entries are placed at the
  * single valid/invalid boundary, then an accepted head issue shifts the
  * staged state by exactly one slot.  Completion wakeups are applied after
  * that shift so the compare network does not sit in front of the payload
  * compactor.
  *
  * Capacity is still evaluated from registered state.  A same-cycle issue
  * does not make an extra dispatch slot available, preserving the accepted
  * N11 dispatch contract and exact benchmark cycle behavior.
  */
final class WeBattleMemIssueQueueState[T <: IssueSlot](
    slotType: HardType[T],
    queue: Vec[Flow[T]],
    pushPorts: Vec[Stream[T]],
    issueReq: Bool,
    issueFire: Bool,
    flush: Bool,
    wakeups: Seq[Flow[UInt]],
    operandPorts: Int
) extends Area {
  private val depth = queue.length
  private val pushWidth = pushPorts.length

  require(depth > 0)
  require(pushWidth > 0 && pushWidth <= depth)
  require(operandPorts > 0)

  // A dense queue has exactly one insertion boundary.  Lane N inserts N
  // positions after that boundary.
  val insertionBoundary = Vec(Bool, depth)
  insertionBoundary(0) := !queue(0).valid
  for (slot <- 1 until depth) {
    insertionBoundary(slot) := queue(slot - 1).valid && !queue(slot).valid
  }

  for (lane <- 0 until pushWidth) {
    // Do not borrow capacity released by a same-cycle head issue.
    pushPorts(lane).ready := !queue(depth - lane - 1).valid
  }

  val staged = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    staged(slot) := queue(slot)
    for (lane <- 0 until pushWidth) {
      if (slot >= lane) {
        when(pushPorts(lane).fire && insertionBoundary(slot - lane)) {
          staged(slot).valid := True
          staged(slot).payload := pushPorts(lane).payload
        }
      }
    }
  }

  val removeHead = issueReq && issueFire
  val compacted = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    compacted(slot) := staged(slot)
    when(removeHead) {
      if (slot + 1 < depth) {
        compacted(slot) := staged(slot + 1)
      } else {
        compacted(slot).valid := False
      }
    }

    queue(slot) := compacted(slot)

    // Fold broadcasts into the final destination slot, including entries
    // accepted during this cycle.
    for (operand <- 0 until operandPorts; wakeup <- wakeups) {
      when(
        compacted(slot).valid &&
          wakeup.valid &&
          wakeup.payload === compacted(slot).payload.rRegs(operand).payload
      ) {
        queue(slot).payload.rRegs(operand).valid := True
      }
    }
  }

  when(flush) {
    for (slot <- 0 until depth) {
      queue(slot).valid := False
    }
  }

  for (slot <- 1 until depth) {
    assert(!queue(slot).valid || queue(slot - 1).valid)
  }
  for (lane <- 1 until pushWidth) {
    assert(!pushPorts(lane).valid || pushPorts(lane - 1).valid)
  }
}
