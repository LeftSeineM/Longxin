package NOP.pipeline.mem

import spinal.core._
import spinal.lib._

import NOP._
import NOP.builder._
import NOP.pipeline._
import NOP.pipeline.core.CommitPlugin
import NOP.utils._

/** Explicit phased state transition for the dense, ordered store buffer.
  *
  * The phases make simultaneous retirement, append, selective flush and pop
  * behavior auditable. The inherited implementation used write ordering
  * between a register vector and a shadow combinational vector.
  */
final class WeBattleStoreBufferState(
    config: MyCPUConfig,
    queue: Vec[Flow[StoreBufferSlot]],
    push: Stream[StoreBufferSlot],
    pop: Stream[StoreBufferSlot],
    commitStore: Bool,
    flush: Bool
) extends Area {
  private val depth = queue.length
  private val slotType = HardType(StoreBufferSlot(config))

  require(depth >= 5, "memory pipeline reserves five potential producers")

  // The public plugin interface already exposes queue(0) on the pop stream.
  push.ready := !queue.last.valid

  val retireBoundary = Vec(Bool, depth)
  retireBoundary(0) := queue(0).valid && !queue(0).retired
  for (slot <- 1 until depth) {
    retireBoundary(slot) :=
      queue(slot).valid &&
        queue(slot - 1).retired &&
        !queue(slot).retired
  }

  val afterRetire = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    afterRetire(slot) := queue(slot)
    when(commitStore && retireBoundary(slot)) {
      afterRetire(slot).payload.retired := True
    }
  }

  val insertionBoundary = Vec(Bool, depth)
  insertionBoundary(0) := !queue(0).valid
  for (slot <- 1 until depth) {
    insertionBoundary(slot) := queue(slot - 1).valid && !queue(slot).valid
  }

  val afterPush = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    afterPush(slot) := afterRetire(slot)
    when(push.fire && insertionBoundary(slot)) {
      afterPush(slot).valid := True
      afterPush(slot).payload := push.payload
    }
  }

  val afterFlush = Vec(Flow(slotType), depth)
  for (slot <- 0 until depth) {
    afterFlush(slot) := afterPush(slot)
    when(flush && !afterPush(slot).payload.retired) {
      afterFlush(slot).valid := False
    }
  }

  for (slot <- 0 until depth) {
    queue(slot) := afterFlush(slot)
    when(pop.fire) {
      if (slot + 1 < depth) {
        queue(slot) := afterFlush(slot + 1)
      } else {
        queue(slot).valid := False
      }
    }
  }

  assert(CountOne(retireBoundary.asBits) <= 1)
  assert(CountOne(insertionBoundary.asBits) <= 1)
  for (slot <- 1 until depth) {
    assert(!queue(slot).valid || queue(slot - 1).valid)
    assert(!queue(slot).retired || queue(slot - 1).retired)
  }
}

/** WeBattle state owner retaining the established forwarding-query interface. */
final class WeBattleStoreBufferPlugin(config: MyCPUConfig)
    extends StoreBufferPlugin(config) {
  override def build(pipeline: MemPipeline): Unit =
    pipeline plug new Area {
      val commit = pipeline.globalService(classOf[CommitPlugin])
      new WeBattleStoreBufferState(
        config,
        queue,
        queueIO.pushPort,
        queueIO.popPort,
        commit.commitStore,
        commit.regFlush
      )
    }
}
