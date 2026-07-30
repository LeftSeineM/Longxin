package NOP.pipeline.exe

import spinal.core._

/** Mutual-exclusion selector for an age-ordered issue queue.
  *
  * Slot zero is the oldest instruction.  Execution port zero has first choice;
  * each later port sees the remaining slots.  A prefix network chooses the
  * first eligible slot without relying on the inherited OHMasking helper or on
  * a binary slot encoder/decoder.
  */
class WeBattleAgeOrderedIssueSelector(depth: Int, portCount: Int)
    extends Area {
  require(depth > 0)
  require(portCount > 0)

  val requests = Vec(Bits(depth bits), portCount)
  val grants = Vec(Bits(depth bits), portCount)

  // allocatedBefore(p) is the union of all grants from older-priority ports.
  val allocatedBefore = Vec(Bits(depth bits), portCount + 1)
  allocatedBefore(0) := 0

  for (port <- 0 until portCount) {
    val eligible = requests(port) & ~allocatedBefore(port)
    val seenOlder = Vec(Bool(), depth + 1)
    seenOlder(0) := False

    for (slot <- 0 until depth) {
      grants(port)(slot) := eligible(slot) && !seenOlder(slot)
      seenOlder(slot + 1) := seenOlder(slot) || eligible(slot)
    }

    allocatedBefore(port + 1) :=
      allocatedBefore(port) | grants(port)
  }
}
