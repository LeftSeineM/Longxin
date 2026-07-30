package NOP.pipeline.decode

import spinal.core._
import spinal.lib._

import NOP._
import NOP.pipeline.core.RegFileMappingEntryBundle

/** Three-wide speculative/committed rename state with an explicit free ring.
  *
  * The free ring contains every physical register not referenced by the
  * committed mapping plus speculative allocations that can be reclaimed.
  * Recovery advances the committed view first, then atomically restores the
  * speculative map and makes the ring full at the updated retirement tail.
  */
final class WeBattleRenameState(
    config: MyCPUConfig,
    sourceArch: Seq[Seq[UInt]],
    destinationArch: Seq[UInt],
    destinationValid: Seq[Bool],
    renameFire: Bool,
    commits: Seq[Flow[RegFileMappingEntryBundle]],
    recover: Bool
) extends Area {
  private val rf = config.regFile
  private val laneCount = config.decode.decodeWidth
  private val commitCount = config.rob.retireWidth
  private val sourceCount = rf.rPortsEachInst
  private val freeDepth = rf.nPhysRegs + 1 - rf.nArchRegs
  private val pointerWidth = log2Up(freeDepth)
  private val countWidth = log2Up(freeDepth + 1)

  require(isPow2(freeDepth))
  require(sourceArch.size == laneCount)
  require(destinationArch.size == laneCount)
  require(destinationValid.size == laneCount)
  require(commits.size == commitCount)

  val speculativeMap = Vec.tabulate(rf.nArchRegs - 1) { index =>
    RegInit(U(index + 1, rf.prfAddrWidth bits))
  }
  val committedMap = Vec.tabulate(rf.nArchRegs - 1) { index =>
    RegInit(U(index + 1, rf.prfAddrWidth bits))
  }
  val freeRing = Vec.tabulate(freeDepth) { index =>
    RegInit(U(index + rf.nArchRegs, rf.prfAddrWidth bits))
  }
  val allocateHead = RegInit(U(0, pointerWidth bits))
  val retireTail = RegInit(U(0, pointerWidth bits))
  val available = RegInit(U(freeDepth, countWidth bits))

  private def readMap(map: Vec[UInt], arch: UInt): UInt =
    Mux(arch === 0, U(0, rf.prfAddrWidth bits), map(arch - 1))

  private val requestedAllocations = CountOne(destinationValid)
  val canAllocate = available >= requestedAllocations.resize(countWidth)

  val sourcePhys = Vec(
    Vec(UInt(rf.prfAddrWidth bits), sourceCount),
    laneCount
  )
  val previousDestination = Vec(UInt(rf.prfAddrWidth bits), laneCount)
  val newDestination = Vec(UInt(rf.prfAddrWidth bits), laneCount)
  val allocation = Vec(Flow(UInt(rf.prfAddrWidth bits)), laneCount)

  for (lane <- 0 until laneCount) {
    val olderWriteCount =
      if (lane == 0) U(0, log2Up(laneCount + 1) bits)
      else CountOne(destinationValid.take(lane))

    for (source <- 0 until sourceCount) {
      sourcePhys(lane)(source) := readMap(speculativeMap, sourceArch(lane)(source))
      for (older <- 0 until lane) {
        when(
          destinationValid(older) &&
            destinationArch(older) === sourceArch(lane)(source) &&
            sourceArch(lane)(source) =/= 0
        ) {
          sourcePhys(lane)(source) := newDestination(older)
        }
      }
    }

    previousDestination(lane) := readMap(speculativeMap, destinationArch(lane))
    for (older <- 0 until lane) {
      when(
        destinationValid(older) &&
          destinationArch(older) === destinationArch(lane) &&
          destinationArch(lane) =/= 0
      ) {
        previousDestination(lane) := newDestination(older)
      }
    }

    newDestination(lane) := Mux(
      destinationValid(lane),
      freeRing((allocateHead + olderWriteCount).resized),
      previousDestination(lane)
    )
    allocation(lane).valid := renameFire && destinationValid(lane)
    allocation(lane).payload := newDestination(lane)
  }

  val allocationCount = CountOne(allocation.map(_.valid))
  val retirement = commits.map(commit =>
    commit.valid && commit.payload.addr =/= 0
  )
  val retirementCount = CountOne(retirement)

  // Retirement writes are dense in program order even when a lane has no
  // architectural destination.
  for (lane <- 0 until commitCount) {
    val olderRetireCount =
      if (lane == 0) U(0, log2Up(commitCount + 1) bits)
      else CountOne(retirement.take(lane))
    when(retirement(lane)) {
      freeRing((retireTail + olderRetireCount).resized) :=
        commits(lane).payload.prevAddr
      committedMap(commits(lane).payload.addr - 1) :=
        commits(lane).payload.prfAddr
    }
  }

  when(recover) {
    // Same-cycle commits are part of the precise state being restored.
    speculativeMap := committedMap
    speculativeMap.foreach(_.allowOverride())
    for (lane <- 0 until commitCount) {
      when(retirement(lane)) {
        speculativeMap(commits(lane).payload.addr - 1) :=
          commits(lane).payload.prfAddr
      }
    }
    retireTail := retireTail + retirementCount
    allocateHead := retireTail + retirementCount
    available := freeDepth
  } otherwise {
    retireTail := retireTail + retirementCount
    allocateHead := allocateHead + allocationCount
    available :=
      available + retirementCount.resize(countWidth) -
        allocationCount.resize(countWidth)

    when(renameFire) {
      for (lane <- 0 until laneCount) {
        when(destinationValid(lane)) {
          speculativeMap(destinationArch(lane) - 1) := newDestination(lane)
        }
      }
    }
  }

  assert(!renameFire || canAllocate)
}
