package NOP.pipeline.fetch

import spinal.core._
import spinal.lib._

/** WeBattle multi-enqueue, multi-dequeue instruction queue.
  *
  * This is an independent implementation for the small register-backed queue
  * between fetch and decode.  It deliberately uses an explicit occupancy
  * count instead of the inherited FIFO's equal-pointer plus phase-bit state.
  *
  * Interface contract:
  *   - valid enqueue lanes and ready dequeue lanes are dense from lane zero;
  *   - enqueue does not consume capacity released in the same cycle;
  *   - dequeue does not bypass data enqueued in the same cycle;
  *   - flush has priority over all pointer, occupancy and storage updates.
  *
  * The no-bypass rules match the frozen N5 queue timing contract and avoid a
  * combinational fetch-to-decode path.  Supporting different enqueue/dequeue
  * widths is required by the four-wide fetch and three-wide decode frontend.
  */
class WeBattleFetchQueue[T <: Data](
    dataType: HardType[T],
    depth: Int,
    enqueueWidth: Int,
    dequeueWidth: Int
) extends Component {
  require(depth > 0 && isPow2(depth))
  require(enqueueWidth > 0 && enqueueWidth <= depth)
  require(dequeueWidth > 0 && dequeueWidth <= depth)

  private val pointerWidth = log2Up(depth)
  private val occupancyWidth = log2Up(depth + 1)

  val io = new Bundle {
    val flush = in(Bool)
    val enqueue = Vec(slave(Stream(dataType)), enqueueWidth)
    val dequeue = Vec(master(Stream(dataType)), dequeueWidth)
    val occupancy = out(UInt(occupancyWidth bits))
  }

  val storage = Reg(Vec(dataType, depth))
  val head = RegInit(U(0, pointerWidth bits))
  val tail = RegInit(U(0, pointerWidth bits))
  val count = RegInit(U(0, occupancyWidth bits))

  io.occupancy := count

  // A lane may accept only when all lower lanes are valid and the queue had
  // enough free entries at the start of this cycle.
  for (lane <- 0 until enqueueWidth) {
    val lowerValid = io.enqueue.take(lane + 1).map(_.valid).andR
    io.enqueue(lane).ready := count <= depth - lane - 1
    when(io.enqueue(lane).ready && lowerValid) {
      // Decode the circular destination explicitly.  A dynamic write index on
      // this wide bundle synthesizes into thousands of LUTs on 7-series FPGAs;
      // the one-hot ring selection shares the small pointer comparison.
      for (slot <- 0 until depth) {
        when(tail === ((slot - lane + depth) % depth)) {
          storage(slot) := io.enqueue(lane).payload
        }
      }
    }
  }

  // Existing entries are presented in program order.  A dequeue lane is
  // valid solely from registered occupancy, so there is no enqueue bypass.
  for (lane <- 0 until dequeueWidth) {
    io.dequeue(lane).valid := count > lane
    io.dequeue(lane).payload.assignDontCare()
    for (slot <- 0 until depth) {
      when(head === ((slot - lane + depth) % depth)) {
        io.dequeue(lane).payload := storage(slot)
      }
    }
  }

  // The first non-firing lane terminates each dense transaction.
  val enqueueCount = PriorityMux(
    io.enqueue.zipWithIndex.map { case (port, lane) =>
      !port.fire -> U(lane, log2Up(enqueueWidth + 1) bits)
    } :+ (True -> U(enqueueWidth, log2Up(enqueueWidth + 1) bits))
  )
  val dequeueCount = PriorityMux(
    io.dequeue.zipWithIndex.map { case (port, lane) =>
      !port.fire -> U(lane, log2Up(dequeueWidth + 1) bits)
    } :+ (True -> U(dequeueWidth, log2Up(dequeueWidth + 1) bits))
  )

  when(io.flush) {
    head := 0
    tail := 0
    count := 0
  } otherwise {
    head := head + dequeueCount.resized
    tail := tail + enqueueCount.resized
    count := (count + enqueueCount.resized - dequeueCount.resized).resized
  }

  // Simulation-time contract checks.  They elaborate away from the functional
  // datapath and make integration mistakes visible before official testing.
  assert(count <= depth)
  for (lane <- 1 until enqueueWidth) {
    assert(!io.enqueue(lane).valid || io.enqueue(lane - 1).valid)
  }
  for (lane <- 1 until dequeueWidth) {
    assert(!io.dequeue(lane).ready || io.dequeue(lane - 1).ready)
  }
}
