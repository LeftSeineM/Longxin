package NOP.pipeline.exe

import spinal.core._

/** Physical-register tag match matrix for reservation-station wakeup.
  *
  * The component is intentionally stateless.  Queue state remains owned by the
  * issue queue; this block provides one wake bit for every slot/operand pair
  * after matching all completion broadcasts in parallel.
  */
class WeBattleWakeupMatrix(
    slotCount: Int,
    operandsPerSlot: Int,
    broadcastCount: Int,
    tagWidth: Int
) extends Area {
  private val operandCount = slotCount * operandsPerSlot

  require(slotCount > 0)
  require(operandsPerSlot > 0)
  require(broadcastCount > 0)

  val operandTags = Vec(UInt(tagWidth bits), operandCount)
  val broadcastValid = Bits(broadcastCount bits)
  val broadcastTags = Vec(UInt(tagWidth bits), broadcastCount)
  val wake = Bits(operandCount bits)

  for (operand <- 0 until operandCount) {
    val matches = Bits(broadcastCount bits)
    for (broadcast <- 0 until broadcastCount) {
      matches(broadcast) :=
        broadcastValid(broadcast) &&
          (broadcastTags(broadcast) === operandTags(operand))
    }
    wake(operand) := matches.orR
  }
}
