package NOP.pipeline.mem

import spinal.core._

import NOP._
import NOP.builder._
import NOP.constants.enum._
import NOP.pipeline._
import NOP.pipeline.priviledge._
import NOP.utils._

/** Store lane selection and alignment checking, isolated from translation. */
final class WeBattleStoreAligner extends Component {
  val io = new Bundle {
    val accessType = in(LoadStoreType())
    val addressLow = in(UInt(2 bits))
    val source = in(BWord())
    val preserveSource = in(Bool())
    val byteEnable = out(Bits(4 bits))
    val alignedData = out(BWord())
    val misaligned = out(Bool())
  }

  io.misaligned := False

  switch(io.accessType) {
    import LoadStoreType._
    is(BYTE, BYTE_U, CACOP, PRELD) {
      io.byteEnable := (B"4'b0001" |<< io.addressLow).resize(4)
      io.alignedData := io.source |<< (io.addressLow << 3)
    }
    is(HALF, HALF_U) {
      io.byteEnable := Mux(io.addressLow(1), B"4'b1100", B"4'b0011")
      io.alignedData := Mux(
        io.addressLow(1),
        io.source |<< 16,
        io.source
      )
      io.misaligned := io.addressLow(0)
    }
    is(WORD) {
      io.byteEnable := B"4'b1111"
      io.alignedData := io.source
      io.misaligned := io.addressLow =/= 0
    }
  }

  when(io.preserveSource) {
    io.alignedData := io.source
  }
}

/** WeBattle-owned effective-address, alignment and translation boundary. */
final class WeBattleAddressGenerationPlugin(config: MyCPUConfig)
    extends AddressGenerationPlugin(config) {
  override def build(pipeline: MemPipeline): Unit = {
    val exceptionState =
      pipeline.globalService(classOf[ExceptionHandlerPlugin])

    pipeline.MEMADDR plug new Area {
      import pipeline.MEMADDR._
      import pipeline.signals._

      val operation = input(ISSUE_SLOT).uop
      val virtualAddress = input(MEMORY_ADDRESS)
      val store = operation.isStore

      val aligner = new WeBattleStoreAligner
      aligner.io.accessType := operation.lsType
      aligner.io.addressLow := virtualAddress(1 downto 0)
      aligner.io.source := input(MEMORY_WRITE_DATA)
      aligner.io.preserveSource := operation.isLoad

      insert(MEMORY_BE) := aligner.io.byteEnable
      output(MEMORY_WRITE_DATA).allowOverride := aligner.io.alignedData
      raiseALE.setWhen(aligner.io.misaligned)

      val saved = TranslateCSRBundle()
      saved.CRMD_DA := exceptionState.CRMD_DA
      saved.CRMD_PG := exceptionState.CRMD_PG
      saved.CRMD_DATF := exceptionState.CRMD_DATF
      saved.CRMD_DATM := exceptionState.CRMD_DATM
      insert(TRANSLATE_SAVED_CSR) := saved

      val mmu = pipeline.globalService(classOf[MMUPlugin])
      val access =
        Mux(store, MemOperationType.STORE, MemOperationType.LOAD)
      val direct = mmu.directTranslate(virtualAddress, access)
      insert(DIRECT_TRANSLATE_RESULT) := direct.resultBundle
      if (!config.translation.contestDirectMode) {
        val translated = mmu.tlbTranslate(virtualAddress, access)
        insert(TLB_TRANSLATE_RESULT) := translated.resultBundle
      }
    }

    pipeline.MEM1 plug new Area {
      import pipeline.MEM1._
      import pipeline.signals._

      val operation = input(ISSUE_SLOT).uop
      val virtualAddress = input(MEMORY_ADDRESS)
      val store = operation.isStore
      val physicalAddress = insert(MEMORY_ADDRESS_PHYSICAL)

      if (config.translation.contestDirectMode) {
        val saved = input(TRANSLATE_SAVED_CSR)
        val direct = input(DIRECT_TRANSLATE_RESULT)
        val paging = !saved.CRMD_DA && saved.CRMD_PG
        val directHit = paging && direct.valid
        val translationMissing = paging && !direct.valid

        insert(ADDRESS_CACHED) :=
          Mux(directHit, direct.payload.cached, saved.CRMD_DATM(0))
        physicalAddress :=
          Mux(directHit, direct.payload.physAddr, virtualAddress)
        insert(IS_TLB_REFILL) := translationMissing

        raisePIL := False
        raisePIS := False
        raisePME := False
        raisePPI := False
        raiseTLBR := translationMissing
      } else {
        val mmu = pipeline.globalService(classOf[MMUPlugin])
        val access =
          Mux(store, MemOperationType.STORE, MemOperationType.LOAD)
        val translated = mmu.translate(
          virtualAddress,
          access,
          input(DIRECT_TRANSLATE_RESULT),
          input(TLB_TRANSLATE_RESULT),
          input(TRANSLATE_SAVED_CSR)
        )

        insert(ADDRESS_CACHED) := translated.resultBundle.cached
        physicalAddress := translated.resultBundle.physAddr
        insert(IS_TLB_REFILL) :=
          translated.resultExceptionBundle.raiseTLBR
        raisePIL := translated.resultExceptionBundle.raisePIL
        raisePIS := translated.resultExceptionBundle.raisePIS
        raisePME := translated.resultExceptionBundle.raisePME
        raisePPI := translated.resultExceptionBundle.raisePPI
        raiseTLBR := translated.resultExceptionBundle.raiseTLBR
      }
    }
  }
}
