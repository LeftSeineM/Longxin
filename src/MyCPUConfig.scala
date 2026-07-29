package NOP

import spinal.core._
import spinal.lib.bus.amba4.axi._
import spinal.lib._

abstract class CacheBasicConfig {
  val sets: Int
  val lineSize: Int
  val ways: Int
  val offsetWidth = log2Up(lineSize)
  val offsetRange = (offsetWidth - 1) downto 0
  val wordOffsetRange = (offsetWidth - 1) downto 2
  val indexWidth = log2Up(sets)
  val indexRange = (offsetWidth + indexWidth - 1) downto offsetWidth
  val tagOffset = offsetWidth + indexWidth
  val tagRange = 31 downto tagOffset
  def wordCount = sets * lineSize / 4
  def lineWords = lineSize / 4
}

final case class ICacheConfig(
    sets: Int = 64,
    lineSize: Int = 64,
    ways: Int = 2,
    useReorder: Boolean = false
) extends CacheBasicConfig {
  require(sets * lineSize <= (4 << 10), "4KB is VIPT limit")
  val enable = true
}

final case class DCacheConfig(
    sets: Int = 64,
    lineSize: Int = 64,
    ways: Int = 2
) extends CacheBasicConfig {
  require(sets * lineSize <= (4 << 10), "4KB is VIPT limit")
  val enable = true
}

final case class BTBConfig(
    sets: Int = 1024,
    lineSize: Int = 4,
    ways: Int = 1,
    rasEntries: Int = 8
) extends CacheBasicConfig {
  val enable = true
}

final case class BPUConfig(
    sets: Int = 1024,
    phtSets: Int = 8192,
    historyWidth: Int = 5,
    counterWidth: Int = 2,
    initialCounter: Int = 1,
    useBankSafeGShare: Boolean = false,
    useTournament: Boolean = false
) {
  require(initialCounter >= 0 && initialCounter < (1 << counterWidth))
  val indexWidth = log2Up(sets)
  val indexRange = 2 until 2 + indexWidth
  val ways = 1 << historyWidth
  val phtIndexWidth = log2Up(phtSets)
  val phtPCRange = 2 until 2 + phtIndexWidth - historyWidth
  val counterType = HardType(UInt(counterWidth bits))
  val historyType = HardType(UInt(historyWidth bits))
  val useGlobal = true
  val useLocal = false
  val useHybrid = false
}

/** Reproducible contest configurations for the WeBattle derivative.
  *
  * WB-N0 keeps the upstream microarchitecture and only applies the 2026 ISA
  * compatibility layer. WB-N1 is the first independently maintained profile:
  * it exposes predictor experiments as reproducible parameters while its
  * frozen defaults keep the validated upstream gselect index and 8-entry RAS.
  */
object WeBattleProfiles {
  def N0: MyCPUConfig = MyCPUConfig(
    debug = true,
    weDebug = false,
    debug_difftest = false
  )

  def N1: MyCPUConfig = MyCPUConfig(
    debug = true,
    weDebug = false,
    debug_difftest = false,
    frontend = FrontendConfig(
      btb = BTBConfig(rasEntries = 8),
      bpu = BPUConfig(
        initialCounter = 1,
        useBankSafeGShare = false
      )
    )
  )

  /** Initial-round profile.
    *
    * The 2026 preliminary ISA excludes TLB/MMU instructions.  N2 therefore
    * removes the full associative translation lookup from the fetch and load/
    * store timing paths at elaboration time.  N1 remains the Linux-capable
    * profile with the complete MMU.
    */
  def N2: MyCPUConfig = N1.copy(
    translation = TranslationConfig(contestDirectMode = true)
  )

  /** N4 is a local physical-timing experiment derived from N2. */
  def N4: MyCPUConfig = N2.copy(
    timing = TimingConfig(dispatchValidMaxFanout = 64)
  )

  def N5: MyCPUConfig = N4.copy(
    frontend = N4.frontend.copy(
      bpu = N4.frontend.bpu.copy(useTournament = true)
    )
  )
}

final case class FrontendConfig(
    pcInit: Long = 0x1c000000L,
    icache: ICacheConfig = ICacheConfig(),
    btb: BTBConfig = BTBConfig(),
    bpu: BPUConfig = BPUConfig(),
    fetchWidth: Int = 4,
    fetchBufferDepth: Int = 8
)

final case class RegFileConfig(
    nArchRegs: Int = 32,
    nPhysRegs: Int = 31 + 32
) {
  val arfAddrWidth = log2Up(nArchRegs)
  val prfAddrWidth = log2Up(nPhysRegs)
  val rPortsEachInst = 2
}

final case class DecodeConfig(
    decodeWidth: Int = 3,
    allUnique: Boolean = false
)

// Issuing
abstract class IssueConfig {
  val issueWidth: Int
  val depth: Int
  val addrWidth = log2Up(depth)
}

final case class IntIssueConfig(
    issueWidth: Int = 3,
    bruIdx: Int = 0,
    csrIdx: Int = 0,
    timerIdx: Int = 1,
    invTLBIdx: Int = 0,
    depth: Int = 7
) extends IssueConfig {
  require(0 <= bruIdx && bruIdx < issueWidth)
  require(0 <= csrIdx && csrIdx < issueWidth)
  require(0 <= timerIdx && timerIdx < issueWidth)
  require(0 <= invTLBIdx && invTLBIdx < issueWidth)
}

final case class MulDivConfig(
    depth: Int = 3,
    multiplyLatency: Int = 2,
    divisionEarlyOutWidth: Int = 16 // set to 0 to disable early out
) extends IssueConfig {
  val issueWidth: Int = 1
  def useDivisionEarlyOut = divisionEarlyOutWidth > 0
}

final case class MemIssueConfig(
    depth: Int = 5
) extends IssueConfig {
  val issueWidth: Int = 1
}

// Commit
final case class ROBConfig(
    robDepth: Int = 32,
    retireWidth: Int = 3
) {
  val robAddressWidth = log2Up(robDepth)
}

// Interrupt
final case class InterruptConfig(
    innerCounterDownEvery: Int = 1
)

// TLB
final case class TLBConfig(
    numEntries: Int = 16,
    physAddrWidth: Int = 32
) {
  val indexWidth = log2Up(numEntries)
  val virtAddrWidth = 32
  val asidWidth = 10
}

/** Selects the address-translation personality.
  *
  * contestDirectMode is a compile-time specialization for the preliminary
  * contest workload, which uses direct addressing and DMW windows but does not
  * require page-table translation. Keeping this in the shared configuration
  * makes the tradeoff explicit and preserves N1 as the full Linux/MMU build.
  */
final case class TranslationConfig(
    contestDirectMode: Boolean = false
)

/** Optional FPGA synthesis guidance. Zero leaves the upstream netlist alone. */
final case class TimingConfig(
    dispatchValidMaxFanout: Int = 0
)

final case class MyCPUConfig(
    axiConfig: Axi4Config = Axi4Config(
      addressWidth = 32,
      dataWidth = 32,
      idWidth = 4,
      useRegion = false,
      useQos = false
    ),
    debug: Boolean = true,
    weDebug: Boolean = true,
    debug_difftest: Boolean = true,
    frontend: FrontendConfig = FrontendConfig(),
    decode: DecodeConfig = DecodeConfig(),
    regFile: RegFileConfig = RegFileConfig(),
    // EXE
    intIssue: IntIssueConfig = IntIssueConfig(),
    mulDiv: MulDivConfig = MulDivConfig(),
    // MEM
    dcache: DCacheConfig = DCacheConfig(),
    memIssue: MemIssueConfig = MemIssueConfig(),
    storeBufferDepth: Int = 8,
    // Commit
    rob: ROBConfig = ROBConfig(),
    // Interrupt
    interrupt: InterruptConfig = InterruptConfig(),
    // TLB
    tlbConfig: TLBConfig = TLBConfig(),
    translation: TranslationConfig = TranslationConfig(),
    timing: TimingConfig = TimingConfig()
)
