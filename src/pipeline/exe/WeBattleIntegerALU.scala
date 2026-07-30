package NOP.pipeline.exe

import NOP.constants.enum._
import NOP.utils._
import spinal.core._

/** WeBattle integer result datapath.
  *
  * Operations are grouped by physical result class (arithmetic, logic,
  * comparison and shift) and selected once at the output.  Architectural alias
  * operations share hardware explicitly.
  */
class WeBattleIntegerALU extends Component {
  val io = new Bundle {
    val src1 = in(UWord())
    val src2 = in(UWord())
    val shiftAmount = in(UInt(5 bits))
    val op = in(ALUOpType())
    val result = out(UWord())
  }

  import ALUOpType._

  val arithmeticResult = UWord()
  val logicResult = UWord()
  val compareResult = UWord()
  val shiftResult = UWord()

  val subtract = io.op === SUB || io.op === SUBU
  arithmeticResult := io.src1 + io.src2
  when(subtract) {
    arithmeticResult := io.src1 - io.src2
  }

  logicResult := 0
  switch(io.op) {
    is(AND) { logicResult := io.src1 & io.src2 }
    is(OR)  { logicResult := io.src1 | io.src2 }
    is(XOR) { logicResult := io.src1 ^ io.src2 }
    is(NOR) { logicResult := ~(io.src1 | io.src2) }
  }

  // Signed ordering is an unsigned comparison after flipping the sign bit.
  val signedKey1 = io.src1 ^ U(0x80000000L, 32 bits)
  val signedKey2 = io.src2 ^ U(0x80000000L, 32 bits)
  compareResult := 0
  when(io.op === SLT) {
    compareResult(0) := signedKey1 < signedKey2
  } elsewhen (io.op === SLTU) {
    compareResult(0) := io.src1 < io.src2
  }

  shiftResult := io.src1 |<< io.shiftAmount
  switch(io.op) {
    is(SRL) { shiftResult := io.src1 |>> io.shiftAmount }
    is(SRA) {
      shiftResult := (io.src1.asSInt >> io.shiftAmount).asUInt
    }
  }

  switch(io.op) {
    is(ADD, ADDU, SUB, SUBU, LU12I, PCADDI, PCADDU12I) {
      io.result := arithmeticResult
    }
    is(AND, OR, XOR, NOR) {
      io.result := logicResult
    }
    is(SLT, SLTU) {
      io.result := compareResult
    }
    is(SLL, SRL, SRA) {
      io.result := shiftResult
    }
    is(CPUCFG) {
      // Unsupported optional CPUCFG capability fields read as zero.
      io.result := 0
    }
  }
}
