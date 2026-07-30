package NOP.pipeline.exe

import NOP.constants.enum._
import NOP.utils._
import spinal.core._

/** Combined comparison, target-generation and redirect decision block.
  *
  * Keeping these functions in one component makes `actualTaken` a single
  * shared term for both target qualification and prediction checking.
  */
class WeBattleBranchExecute extends Component {
  val io = new Bundle {
    val src1 = in(UWord())
    val src2 = in(UWord())
    val compareOp = in(CompareOpType())

    val predictTaken = in(Bool())
    val predictTarget = in(UWord())
    val isBranch = in(Bool())
    val isJump = in(Bool())
    val isIndirectJump = in(Bool())
    val pc = in(UWord())
    val inst = in(Bits(32 bits))

    val condition = out(Bool())
    val actualTaken = out(Bool())
    val actualTarget = out(UWord())
    val mispredict = out(Bool())
  }

  import CompareOpType._

  val equal = io.src1 === io.src2
  val signedLess = io.src1.asSInt < io.src2.asSInt
  val unsignedLess = io.src1 < io.src2
  val zero = !io.src1.orR
  val negative = io.src1.msb

  switch(io.compareOp) {
    is(EQ)  { io.condition := equal }
    is(NE)  { io.condition := !equal }
    is(EQZ) { io.condition := zero }
    is(NEZ) { io.condition := !zero }
    is(GE)  { io.condition := !signedLess }
    is(LT)  { io.condition := signedLess }
    is(LE)  { io.condition := signedLess || equal }
    is(GT)  { io.condition := !signedLess && !equal }
    is(GEU) { io.condition := !unsignedLess }
    is(LTU) { io.condition := unsignedLess }
    is(LEU) { io.condition := unsignedLess || equal }
    is(GTU) { io.condition := !unsignedLess && !equal }
    is(GEZ) { io.condition := !negative }
    is(LTZ) { io.condition := negative }
    is(LEZ) { io.condition := negative || zero }
    is(GTZ) { io.condition := !negative && !zero }
  }

  val branchOffset =
    (io.inst(25 downto 10).asSInt @@ U(0, 2 bits))
      .resize(32 bits)
      .asUInt
  val directOffset =
    (io.inst(9 downto 0).asSInt @@
      io.inst(25 downto 10).asSInt @@
      U(0, 2 bits)).resize(32 bits).asUInt
  val indirectOffset =
    (io.inst(25 downto 10).asUInt @@ U(0, 2 bits))
      .asSInt
      .resize(32 bits)
      .asUInt

  io.actualTarget := io.src1 + indirectOffset
  when(io.isBranch) {
    io.actualTarget := io.pc + branchOffset
  } elsewhen (io.isJump) {
    io.actualTarget := io.pc + directOffset
  }

  io.actualTaken :=
    (io.isBranch && io.condition) || io.isJump || io.isIndirectJump
  val controlInstruction = io.isBranch || io.isJump || io.isIndirectJump
  val directionWrong = io.predictTaken =/= io.actualTaken
  val targetWrong = io.actualTaken && (io.predictTarget =/= io.actualTarget)

  io.mispredict := io.predictTaken
  when(controlInstruction) {
    io.mispredict := directionWrong || targetWrong
  }
}
