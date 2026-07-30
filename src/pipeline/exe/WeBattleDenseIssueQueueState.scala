package NOP.pipeline.exe

import spinal.core._
import spinal.lib._

import NOP.pipeline.IssueSlot

/** Dense issue-queue state transition shared by the integer, memory and MDU
  * reservation stations.
  *
  * This is deliberately a single next-state network instead of the inherited
  * sequence of enqueue writes followed by prefix-pop overwrites. Accepted
  * dispatch payloads are staged at the registered tail, issued entries are
  * removed by a bounded shift network, and completion wakeups are folded into
  * the entries after they reach their final destination slots.
  *
  * Capacity is evaluated from registered occupancy and does not borrow slots
  * released by same-cycle issue. This keeps dispatch ready independent of the
  * wakeup/select cone and matches the accepted N9 pipeline contract.
  */
final class WeBattleDenseIssueQueueState[T <: IssueSlot](
    slotType: HardType[T],
    queue: Vec[Flow[T]],
    pushPorts: Vec[Stream[T]],
    issueMask: Bits,
    issueFire: Bool,
    flush: Bool,
    wakeups: Seq[Flow[UInt]],
    operandPorts: Int,
    maxIssues: Int
) extends Area {
  private val depth = queue.length
  private val pushWidth = pushPorts.length
  private val occupancyWidth = log2Up(depth + 1)

  require(depth > 0)
  require(pushWidth > 0 && pushWidth <= depth)
  require(issueMask.getWidth == depth)
  require(operandPorts > 0)
  require(maxIssues > 0 && maxIssues <= depth)

  private def prefixCount(events: Seq[Bool], width: Int): UInt =
    if (events.isEmpty) U(0, log2Up(width + 1) bits)
    else CountOne(events)

  val validMask = Bits(depth bits)
  val resident = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    validMask(slot) := queue(slot).valid
    resident(slot) := queue(slot)
  }

  val occupancy = CountOne(validMask)
  for (lane <- 0 until pushWidth) {
    // Dispatch lanes are dense; lane N is ready only when the queue had room
    // for all lanes 0..N at the beginning of the cycle.
    pushPorts(lane).ready := occupancy <= depth - lane - 1
  }

  val acceptedPush = Vec(Bool, pushWidth)
  val pushRank = Vec(UInt(log2Up(pushWidth + 1) bits), pushWidth)
  val normalizedPush = Vec(slotType, pushWidth)
  for (lane <- 0 until pushWidth) {
    acceptedPush(lane) := pushPorts(lane).fire
    pushRank(lane) := prefixCount(acceptedPush.take(lane), pushWidth).resized
    normalizedPush(lane) := pushPorts(lane).payload
  }

  // Stage accepted pushes at the registered tail. Capacity never borrows a
  // same-cycle issue, so every accepted lane has a unique position here.
  // Compaction below then treats resident and newly accepted entries alike.
  val staged = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    staged(slot) := resident(slot)
    for (lane <- 0 until pushWidth) {
      when(
        acceptedPush(lane) &&
          (occupancy.resize(occupancyWidth) +
            pushRank(lane).resize(occupancyWidth)) === slot
      ) {
        staged(slot).valid := True
        staged(slot).payload := normalizedPush(lane)
      }
    }
  }

  // At most maxIssues entries disappear in one cycle. A destination can
  // therefore only select itself or one of the next maxIssues slots. This
  // bounded shift network is much shallower than comparing every survivor
  // rank against every destination.
  val removedPrefix =
    Vec(UInt(log2Up(maxIssues + 1) bits), depth)
  removedPrefix(0) :=
    (issueFire && issueMask(0) && queue(0).valid).asUInt.resized
  for (slot <- 1 until depth) {
    removedPrefix(slot) :=
      removedPrefix(slot - 1) +
        (issueFire && issueMask(slot) && queue(slot).valid).asUInt
  }

  val compacted = Vec(Flow(slotType), depth)
  for (destination <- 0 until depth) {
    compacted(destination) := staged(destination)
    for (shift <- 1 to maxIssues) {
      if (destination + shift < depth) {
        when(removedPrefix(destination + shift - 1) === shift) {
          compacted(destination) := staged(destination + shift)
        }
      } else {
        when(removedPrefix.last === shift) {
          compacted(destination).valid := False
        }
      }
    }

    queue(destination) := compacted(destination)

    // Apply wakeups after the payload has reached its final destination.
    // This keeps the issue-select/compaction cone from sitting behind every
    // completion-tag comparison, while still folding broadcasts into entries
    // accepted on this same edge.
    for (operand <- 0 until operandPorts; wakeup <- wakeups) {
      when(
        compacted(destination).valid &&
          wakeup.valid &&
          wakeup.payload === compacted(destination).payload
            .rRegs(operand)
            .payload
      ) {
        queue(destination).payload.rRegs(operand).valid := True
      }
    }
  }

  when(flush) {
    for (slot <- 0 until depth) {
      queue(slot).valid := False
    }
  }

  assert(occupancy <= depth)
  for (slot <- 1 until depth) {
    assert(!queue(slot).valid || queue(slot - 1).valid)
  }
  for (lane <- 1 until pushWidth) {
    assert(!pushPorts(lane).valid || pushPorts(lane - 1).valid)
  }
}
