package NOP.pipeline.exe

import spinal.core._
import spinal.lib._

import NOP._
import NOP.blackbox.execute.Multiplier
import NOP.utils._

final case class WeBattleMulDivCommand() extends Bundle {
  val multiply = Bool()
  val highOrRemainder = Bool()
  val signed = Bool()
  val lhs = Bits(32 bits)
  val rhs = Bits(32 bits)
}

/** One-command multiply/divide engine.
  *
  * The Xilinx multiplier and Spinal unsigned-divider arithmetic primitives are
  * retained, while command capture, signed magnitude conversion, early-path
  * selection, completion lifetime and result correction are owned here. A
  * held EXE stage cannot reissue its command because cmd.ready drops as soon
  * as the command is captured.
  */
final class WeBattleMulDivUnit(config: MulDivConfig) extends Component {
  require(config.multiplyLatency >= 1)

  val io = new Bundle {
    val flush = in(Bool)
    val cmd = slave(Stream(WeBattleMulDivCommand()))
    val rsp = master(Stream(Bits(32 bits)))
    val busy = out(Bool)
    val wakeup = out(Bool)
  }

  private val counterWidth = log2Up(config.multiplyLatency + 1)
  private val active = RegInit(False)
  private val multiply = Reg(Bool()) init (False)
  private val highOrRemainder = Reg(Bool()) init (False)
  private val resultNegative = Reg(Bool()) init (False)
  private val remainderNegative = Reg(Bool()) init (False)
  private val useSmallDivider = Reg(Bool()) init (False)
  private val lhsMagnitude = Reg(UInt(32 bits)) init (0)
  private val rhsMagnitude = Reg(UInt(32 bits)) init (0)
  private val multiplyCounter = Reg(UInt(counterWidth bits)) init (0)

  val incomingLhs = io.cmd.lhs.asSInt
  val incomingRhs = io.cmd.rhs.asSInt
  val incomingLhsMagnitude = incomingLhs.abs(io.cmd.signed)
  val incomingRhsMagnitude = incomingRhs.abs(io.cmd.signed)
  val incomingSmall =
    if (config.useDivisionEarlyOut) {
      val width = config.divisionEarlyOutWidth
      incomingLhsMagnitude(31 downto width) === 0 &&
      incomingRhsMagnitude(31 downto width) === 0
    } else False

  io.cmd.ready := !active && !io.flush
  io.busy := active

  when(io.cmd.fire) {
    active := True
    multiply := io.cmd.multiply
    highOrRemainder := io.cmd.highOrRemainder
    resultNegative := io.cmd.signed && (incomingLhs.sign ^ incomingRhs.sign)
    remainderNegative := io.cmd.signed && incomingLhs.sign
    useSmallDivider := incomingSmall
    lhsMagnitude := incomingLhsMagnitude
    rhsMagnitude := incomingRhsMagnitude
    // The multiplier samples its input on this same edge.  Count command
    // acceptance as pipeline cycle one instead of inserting a capture bubble.
    multiplyCounter := Mux(io.cmd.multiply, U(1, counterWidth bits), U(0))
  }

  val multiplier = new Multiplier()
  multiplier.io.A :=
    Mux(io.cmd.fire, incomingLhsMagnitude, lhsMagnitude)
  multiplier.io.B :=
    Mux(io.cmd.fire, incomingRhsMagnitude, rhsMagnitude)
  when(active && multiply && multiplyCounter =/= config.multiplyLatency) {
    multiplyCounter := multiplyCounter + 1
  }
  val multiplyDone =
    active && multiply && multiplyCounter === config.multiplyLatency
  val correctedProduct =
    multiplier.io.P.twoComplement(resultNegative).asBits
  val multiplyResult =
    Mux(highOrRemainder, correctedProduct(63 downto 32), correctedProduct(31 downto 0))

  val divider = new math.UnsignedDivider(32, 32, false)
  divider.io.flush := io.flush
  divider.io.cmd.valid := io.cmd.fire && !io.cmd.multiply && !incomingSmall
  divider.io.cmd.numerator := incomingLhsMagnitude
  divider.io.cmd.denominator := incomingRhsMagnitude

  val quotientMagnitude = UInt(32 bits)
  val remainderMagnitude = UInt(32 bits)
  val divideDone = Bool()
  quotientMagnitude := divider.io.rsp.quotient
  remainderMagnitude := divider.io.rsp.remainder
  divideDone := active && !multiply && !useSmallDivider && divider.io.rsp.valid
  divider.io.rsp.ready :=
    io.rsp.ready && active && !multiply && !useSmallDivider

  if (config.useDivisionEarlyOut) {
    val width = config.divisionEarlyOutWidth
    val smallDivider = new math.UnsignedDivider(width, width, false)
    smallDivider.io.flush := io.flush
    smallDivider.io.cmd.valid :=
      io.cmd.fire && !io.cmd.multiply && incomingSmall
    smallDivider.io.cmd.numerator := incomingLhsMagnitude.resized
    smallDivider.io.cmd.denominator := incomingRhsMagnitude.resized
    smallDivider.io.rsp.ready :=
      io.rsp.ready && active && !multiply && useSmallDivider

    when(useSmallDivider) {
      quotientMagnitude := smallDivider.io.rsp.quotient.resized
      remainderMagnitude := smallDivider.io.rsp.remainder.resized
      divideDone := active && !multiply && smallDivider.io.rsp.valid
    }
  }

  val quotient = quotientMagnitude
    .twoComplement(resultNegative)
    .asBits(0, 32 bits)
  val remainder = remainderMagnitude
    .twoComplement(remainderNegative)
    .asBits(0, 32 bits)
  val divideResult = Mux(highOrRemainder, remainder, quotient)

  io.rsp.valid := multiplyDone || divideDone
  io.rsp.payload := Mux(multiply, multiplyResult, divideResult)
  // Match the inherited backend's one-cycle-early multiply wakeup so dependent
  // instructions may read the PRF on the writeback edge.
  io.wakeup :=
    (active && multiply &&
      multiplyCounter === config.multiplyLatency - 1) ||
      divideDone

  when(io.rsp.fire) {
    active := False
  }
  when(io.flush) {
    active := False
    multiplyCounter := 0
  }

  assert(!io.cmd.fire || !active)
  assert(!io.rsp.valid || active)
}
