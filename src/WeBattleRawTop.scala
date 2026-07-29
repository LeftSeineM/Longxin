package NOP

import NOP.pipeline.core._
import NOP.pipeline.fetch.ICachePlugin
import NOP.pipeline.mem.DCachePlugin
import NOP.utils.Axi4Rename
import spinal.core._
import spinal.lib._
import spinal.lib.bus.amba4.axi._

/** Contest-oriented top level maintained by the WeBattle team.
  *
  * Unlike the upstream MyCPU top, this module does not export the very large
  * Difftest, RAT, PRF and cache-observation bundles. It keeps only the AXI
  * master, the architectural retire stream needed by the 2026 wrapper, and
  * explicit performance events. This makes the generated RTL smaller and
  * prevents accidental timing/resource distortion from debug-only logic.
  */
class WeBattleRawTop(config: MyCPUConfig, withPerfEvents: Boolean = true) extends Component {
  require(config.debug, "retire write data requires the compact debug readback path")
  setDefinitionName("wb_raw_top")
  noIoPrefix()

  val io = new Bundle {
    val aclk = in(Bool())
    val aresetn = in(Bool())
    val intrpt = in(Bits(8 bits))

    val axi = master(Axi4(config.axiConfig)).setName("")
    val wid = out(UInt(4 bits))

    val break_point = in(Bool())
    val infor_flag = in(Bool())
    val reg_num = in(UInt(5 bits))
    val ws_valid = out(Bool())
    val rf_rdata = out(Bits(32 bits))

    val retireMask = out(Bits(config.rob.retireWidth bits))
    val retireAddr = out(Vec(Bits(32 bits), config.rob.retireWidth))
    val retireInst = out(Vec(Bits(32 bits), config.rob.retireWidth))
    val retireWen = out(Bits(config.rob.retireWidth bits))
    val retireWaddr = out(Vec(Bits(5 bits), config.rob.retireWidth))
    val retireWresult = out(Vec(Bits(32 bits), config.rob.retireWidth))

    val retireMemMask = withPerfEvents generate out(Bits(config.rob.retireWidth bits))
    val retireBranchMask = withPerfEvents generate out(Bits(config.rob.retireWidth bits))
    val retirePredMask = withPerfEvents generate out(Bits(config.rob.retireWidth bits))
    val retireMispredMask = withPerfEvents generate out(Bits(config.rob.retireWidth bits))
    val icacheMiss = withPerfEvents generate out(Bool())
    val dcacheMiss = withPerfEvents generate out(Bool())

    val exceptionValid = out(Bool())
    val exceptionCode = out(Bits(6 bits))
  }

  val defaultClockDomain = ClockDomain(
    clock = io.aclk,
    reset = io.aresetn,
    config = ClockDomainConfig(resetActiveLevel = LOW)
  )

  val defaultClockArea = new ClockingArea(defaultClockDomain) {
    val cpu = new MyCPUCore(config)
    cpu.io.intrpt := io.intrpt

    val crossbar = new NOP.peripheral.AxiCrossbar(config.axiConfig)
    cpu.io.iBus >> crossbar.io.iBus
    cpu.io.dBus >> crossbar.io.dBus
    crossbar.io.cpuBus >> io.axi

    val axiBuffer = new NOP.peripheral.AxiBuffer(config.axiConfig)
    cpu.io.udBus <> axiBuffer.io.in_axi
    axiBuffer.io.out_axi >> crossbar.io.udBus
    io.wid := RegNextWhen(io.axi.aw.id, io.axi.aw.valid, U(0))

    val rob = cpu.service(classOf[ROBFIFOPlugin])
    val retire = rob.debug_fifoIO.pop
    val retireFire = retire.map(entry => entry.valid && entry.ready)

    io.retireMask := Vec(retireFire).asBits
    io.retireAddr := Vec(retire.map(_.payload.info.uop.pc.asBits))
    io.retireInst := Vec(retire.map(_.payload.info.uop.inst.asBits))
    io.retireWen := Vec(retire.map(entry =>
      entry.valid && entry.ready && entry.payload.info.uop.doRegWrite
    )).asBits
    io.retireWaddr := Vec(retire.map(_.payload.info.uop.wbAddr.asBits))
    io.retireWresult := Vec(rob.debugPopWriteData)

    if (withPerfEvents) {
      io.retireMemMask := Vec(retire.map(entry =>
        entry.valid && entry.ready &&
          (entry.payload.info.uop.isLoad || entry.payload.info.uop.isStore)
      )).asBits
      io.retireBranchMask := Vec(retire.map(entry =>
        entry.valid && entry.ready && entry.payload.info.uop.branchLike
      )).asBits
      io.retirePredMask := Vec(retire.map(entry =>
        entry.valid && entry.ready && entry.payload.info.uop.branchLike &&
          entry.payload.info.uop.predInfo.predictBranch
      )).asBits
      io.retireMispredMask := Vec(retire.map(entry =>
        entry.valid && entry.ready && entry.payload.info.uop.branchLike &&
          entry.payload.state.mispredict
      )).asBits

      io.icacheMiss := cpu.fetchPipeline.service(classOf[ICachePlugin]).missEvent
      io.dcacheMiss := cpu.memPipeline.service(classOf[DCachePlugin]).missEvent
    }

    val commit = cpu.service(classOf[CommitPlugin])
    io.exceptionValid := commit.except.valid
    io.exceptionCode := commit.except.payload.code
  }

  // The 2026 interface keeps these legacy debug controls, but the contest
  // environment does not require interactive register reads.
  io.ws_valid := False
  io.rf_rdata := 0

  addPrePopTask { () =>
    Axi4Rename.Rename(io)
  }
}

object WeBattleMain {
  def main(args: Array[String]): Unit = {
    val profileName = args.headOption.getOrElse("N1").toUpperCase
    val withPerfEvents = !args.drop(1).exists(_.equalsIgnoreCase("synth"))
    val config = profileName match {
      case "N0" => WeBattleProfiles.N0
      case "N1" => WeBattleProfiles.N1
      case "N2" => WeBattleProfiles.N2
      case "N4" => WeBattleProfiles.N4
      case "N5" => WeBattleProfiles.N5
      case "N6" => WeBattleProfiles.N6
      case other => throw new IllegalArgumentException(s"Unknown WeBattle profile: $other")
    }

    SpinalConfig(
      targetDirectory = s"./build/wb-${profileName.toLowerCase}${if (withPerfEvents) "" else "-synth"}",
      headerWithDate = true
    ).generateVerilog(new WeBattleRawTop(config, withPerfEvents))
  }
}
