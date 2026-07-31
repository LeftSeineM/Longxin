package NOP.pipeline.mem

import spinal.core._

import NOP.constants.enum.LoadStoreType
import NOP.builder._
import NOP.pipeline._
import NOP.utils._

/** Shared byte/half/word extraction datapath for cached and uncached loads. */
final class WeBattleLoadAligner extends Component {
  val io = new Bundle {
    val readWord = in(BWord())
    val offset = in(UInt(2 bits))
    val accessType = in(LoadStoreType())
    val result = out(BWord())
  }

  val selectedByte = io.offset.muxList(
    Seq.tabulate(4)(lane => lane -> io.readWord(lane * 8, 8 bits))
  )
  val selectedHalf = Mux(
    io.offset(1),
    io.readWord(16, 16 bits),
    io.readWord(0, 16 bits)
  )

  io.result := io.readWord
  switch(io.accessType) {
    import LoadStoreType._
    is(BYTE) {
      io.result := selectedByte.asSInt.resize(32 bits).asBits
    }
    is(BYTE_U) {
      io.result := selectedByte.asUInt.resize(32 bits).asBits
    }
    is(HALF) {
      io.result := selectedHalf.asSInt.resize(32 bits).asBits
    }
    is(HALF_U) {
      io.result := selectedHalf.asUInt.resize(32 bits).asBits
    }
    is(WORD) {
      io.result := io.readWord
    }
  }
}

/** WeBattle-owned load result selection and extension boundary. */
final class WeBattleLoadPostprocessPlugin extends LoadPostprocessPlugin {
  override def build(pipeline: MemPipeline): Unit =
    pipeline.MEM2 plug new Area {
      import pipeline.MEM2._
      import pipeline.signals._

      val storeData = input(STD_SLOT)
      val useBufferedLoad = storeData.valid && !storeData.isStore

      val selectedType = LoadStoreType()
      val selectedOffset = UInt(2 bits)
      selectedType := input(LOAD_STORE_TYPE)
      selectedOffset := input(MEMORY_ADDRESS_PHYSICAL)(1 downto 0)
      when(useBufferedLoad) {
        selectedType := storeData.lsType
        selectedOffset := storeData.addr(1 downto 0)
      }

      val aligner = new WeBattleLoadAligner
      aligner.io.readWord := input(MEMORY_READ_DATA)
      aligner.io.offset := selectedOffset
      aligner.io.accessType := selectedType

      output(MEMORY_READ_DATA).allowOverride := aligner.io.result
    }
}
