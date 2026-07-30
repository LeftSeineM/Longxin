package NOP.pipeline.core

import spinal.core._
import spinal.lib._

import NOP.RegFileConfig

/** Centralized physical-register value and readiness state.
  *
  * Allocation, completion and value writes are reduced once per physical
  * register.  Read bypass is derived from the same write/clear broadcasts, so
  * queue wakeup and operand consumption observe one coherent event set.
  */
final class WeBattlePhysicalRegisterState(
    config: RegFileConfig,
    registers: Vec[Bits],
    busy: Vec[Bool],
    allocations: Seq[Flow[UInt]],
    writes: Seq[PRFWritePort],
    clears: Seq[Flow[UInt]],
    reads: Seq[(UInt, Bits)],
    busyReads: Seq[(UInt, Bool)]
) extends Area {
  private val count = config.nPhysRegs

  for (index <- 0 until count) {
    val physicalAddress = U(index + 1, config.prfAddrWidth bits)
    val writeHits = writes.map(port =>
      port.hw.valid && port.hw.payload.addr === physicalAddress
    )
    val clearHits = clears.map(port =>
      port.valid && port.payload === physicalAddress
    )
    val allocateHits = allocations.map(port =>
      port.valid && port.payload === physicalAddress
    )

    when(writeHits.orR) {
      registers(index) := MuxOH(
        writeHits.toIndexedSeq,
        writes.map(_.hw.payload.data)
      )
    }
    when(clearHits.orR) {
      busy(index) := False
    }
    when(allocateHits.orR) {
      busy(index) := True
    }

    assert(CountOne(writeHits) <= 1)
    assert(CountOne(allocateHits) <= 1)
  }

  val bypassWrites = writes.filter(_.bypass)
  reads.foreach { case (address, value) =>
    value.allowOverride()
    val hits = bypassWrites.map(port =>
      port.hw.valid && port.hw.payload.addr === address
    )
    when(hits.orR) {
      value := MuxOH(
        hits.toIndexedSeq,
        bypassWrites.map(_.hw.payload.data)
      )
    }
  }

  busyReads.foreach { case (address, value) =>
    value.allowOverride()
    when(clears.map(port =>
      port.valid && port.payload === address
    ).orR) {
      value := False
    }
  }
}
