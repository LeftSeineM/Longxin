package NOP.pipeline.core

import spinal.core._
import spinal.lib._

import NOP._
import NOP.constants.`enum`.LoadStoreType

/** Dense, explicitly-counted reorder-buffer storage.
  *
  * This implementation deliberately does not depend on the inherited
  * MultiPortFIFO helpers.  The queue contract is:
  *
  *   - push and pop lanes are dense from lane zero;
  *   - a full ROB does not accept a same-cycle retire as new capacity;
  *   - flush atomically empties the queue;
  *   - random completion writes bypass to retiring entries in the same cycle.
  *
  * It is an Area instead of a Component so the completion bypass network can
  * be optimized together with the surrounding commit logic.
  */
final class WeBattleROBQueueIO(config: MyCPUConfig) extends Bundle {
  val push = Vec(Stream(ROBEntryBundle(config, false)), config.decode.decodeWidth)
  val pop = Vec(Stream(ROBEntryBundle(config)), config.rob.retireWidth)
  val flush = Bool()
}

final class WeBattleROBStorage(
    config: MyCPUConfig,
    fifo: WeBattleROBQueueIO,
    aluUpdates: Seq[Flow[ROBStateALUPortBundle]],
    branchUpdates: Seq[Flow[ROBStateBRUPortBundle]],
    lsuUpdates: Seq[Flow[ROBStateLSUPortBundle]],
    completeUpdates: Seq[Flow[UInt]]
) extends Area {
  private val depth = config.rob.robDepth
  private val addressWidth = config.rob.robAddressWidth
  private val pushWidth = config.decode.decodeWidth
  private val popWidth = config.rob.retireWidth
  private val countWidth = log2Up(depth + 1)

  require(isPow2(depth))

  val info = Reg(Vec(ROBEntryInfoBundle(config), depth))
  val state = Reg(Vec(ROBEntryStateBundle(config.regFile), depth))
  val pushPtr = RegInit(U(0, addressWidth bits))
  val popPtr = RegInit(U(0, addressWidth bits))
  val occupancy = RegInit(U(0, countWidth bits))

  private def prefixCount(events: Seq[Bool], width: Int): UInt =
    PriorityMux(events.zipWithIndex.map { case (event, index) =>
      !event -> U(index, log2Up(width + 1) bits)
    } :+ (True -> U(width, log2Up(width + 1) bits)))

  private def emptyState(): ROBEntryStateBundle = {
    val value = ROBEntryStateBundle(config.regFile)
    value.complete := False
    value.except.setIdle()
    value.mispredict := False
    value.actualTaken := False
    value.lsuUncached := False
    value.intResult := 0
    value.isCount := False
    value.count64ReadValue := 0
    value.csrRstat := False
    value.csrRdata := 0
    value.isLoad := False
    value.isStore := False
    value.isLL := False
    value.isSC := False
    value.lsType := LoadStoreType.WORD
    value.vAddr := 0
    value.pAddr := 0
    value.storeData := 0
    value.myPC := B(0x0eadbeef, 32 bits).asUInt
    value
  }

  // Pop visibility and dense ready/valid accounting.
  for (lane <- 0 until popWidth) {
    val port = fifo.pop(lane)
    val index = (popPtr + lane).resized
    port.valid := occupancy > lane
    port.payload.info := info(index)
    port.payload.state := state(index)
    port.payload.state.allowOverride()
  }
  val popFire = fifo.pop.map(_.fire)
  val popCount = prefixCount(popFire, popWidth)

  // Do not use same-cycle pops to make a full queue ready.  This keeps the
  // allocation path short and matches the established pipeline contract.
  val freeSlots = U(depth, countWidth bits) - occupancy
  for (lane <- 0 until pushWidth) {
    val port = fifo.push(lane)
    port.ready := freeSlots > lane
    when(port.fire) {
      val index = (pushPtr + lane).resized
      val initial = emptyState()
      initial.allowOverride()
      initial.assignSomeByName(port.payload.state)
      info(index) := port.payload.info
      state(index).allowOverride()
      state(index) := initial
    }
  }
  val pushFire = fifo.push.map(_.fire)
  val pushCount = prefixCount(pushFire, pushWidth)

  // Random writeback.  A completion owns the whole dynamic state record, then
  // overlays the fields carried by that functional unit.
  aluUpdates.foreach { update =>
    when(update.valid) {
      val next = emptyState()
      next.allowOverride()
      next.complete := True
      next.assignSomeByName(update.payload)
      state(update.robIdx).allowOverride()
      state(update.robIdx) := next
    }
  }
  branchUpdates.foreach { update =>
    when(update.valid) {
      val next = emptyState()
      next.allowOverride()
      next.complete := True
      next.assignSomeByName(update.payload)
      state(update.robIdx).allowOverride()
      state(update.robIdx) := next
    }
  }
  lsuUpdates.foreach { update =>
    when(update.valid) {
      val next = emptyState()
      next.allowOverride()
      next.complete := True
      next.assignSomeByName(update.payload)
      state(update.robIdx).allowOverride()
      state(update.robIdx) := next
    }
  }
  completeUpdates.foreach { update =>
    when(update.valid) {
      val next = emptyState()
      next.allowOverride()
      next.complete := True
      state(update.payload).allowOverride()
      state(update.payload) := next
    }
  }

  // Same-cycle writeback-to-retire bypass.  Priority follows registration
  // order, exactly like the state array's last-assignment-wins behavior.
  for (lane <- 0 until popWidth) {
    val port = fifo.pop(lane)
    val index = (popPtr + lane).resized
    aluUpdates.foreach { update =>
      when(update.valid && update.robIdx === index) {
        port.payload.state := emptyState()
        port.payload.state.allowOverride()
        port.payload.state.complete := True
        port.payload.state.assignSomeByName(update.payload)
      }
    }
    branchUpdates.foreach { update =>
      when(update.valid && update.robIdx === index) {
        port.payload.state := emptyState()
        port.payload.state.allowOverride()
        port.payload.state.complete := True
        port.payload.state.assignSomeByName(update.payload)
      }
    }
    lsuUpdates.foreach { update =>
      when(update.valid && update.robIdx === index) {
        port.payload.state := emptyState()
        port.payload.state.allowOverride()
        port.payload.state.complete := True
        port.payload.state.assignSomeByName(update.payload)
      }
    }
    completeUpdates.foreach { update =>
      when(update.valid && update.payload === index) {
        port.payload.state := emptyState()
        port.payload.state.allowOverride()
        port.payload.state.complete := True
      }
    }
  }

  when(fifo.flush) {
    pushPtr := 0
    popPtr := 0
    occupancy := 0
  } otherwise {
    pushPtr := pushPtr + pushCount
    popPtr := popPtr + popCount
    occupancy := occupancy + pushCount.resize(countWidth) - popCount.resize(countWidth)
  }

  assert(pushCount <= freeSlots)
  assert(popCount <= occupancy)
}
