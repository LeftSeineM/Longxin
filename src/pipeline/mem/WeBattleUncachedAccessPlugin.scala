package NOP.pipeline.mem

import spinal.core._
import spinal.lib._
import spinal.lib.fsm._

import NOP._
import NOP.builder._
import NOP.constants.enum.LoadStoreType
import NOP.pipeline._

/** WeBattle-owned uncached-store AXI controller.
  *
  * Address and data channels are tracked independently. A new command can be
  * captured only when the previous B response has completed.
  */
final class WeBattleUncachedAccessPlugin(config: MyCPUConfig)
    extends UncachedAccessPlugin(config) {
  override def build(pipeline: MemPipeline): Unit =
    pipeline.MEM2 plug new Area {
      import pipeline.MEM2._
      import pipeline.signals._

      val buffered = input(STD_SLOT)
      val uncachedStore =
        buffered.valid && buffered.isStore && !buffered.isCached

      when(uncachedStore) {
        uncachedStoreHandshake.valid := arbitration.notStuck
        arbitration.haltItself.setWhen(!uncachedStoreHandshake.ready)
      }

      val controller = new StateMachine {
        disableAutoStart()
        setEntry(stateBoot)

        val sendCommand = new State
        val command = RegNextWhen(
          buffered.payload,
          uncachedStoreHandshake.fire
        )
        val addressAccepted = RegInit(False)
        val dataAccepted = RegInit(False)
        val responseComplete = RegInit(True)

        addressAccepted.setWhen(udBus.aw.fire)
        dataAccepted.setWhen(udBus.w.fire)
        responseComplete.setWhen(udBus.b.fire)
        uncachedStoreHandshake.setBlocked()

        stateBoot.whenIsActive {
          uncachedStoreHandshake.ready :=
            responseComplete || udBus.b.fire
          when(uncachedStoreHandshake.fire) {
            addressAccepted := False
            dataAccepted := False
            responseComplete := False
            goto(sendCommand)
          }
        }

        sendCommand.whenIsActive {
          udBus.aw.valid := !addressAccepted
          udBus.aw.payload.id := 2
          udBus.aw.payload.addr := command.addr
          udBus.aw.payload.len := 0
          udBus.aw.payload.size := LoadStoreType.toAxiSize(command.lsType)
          udBus.aw.payload.burst := B"2'b01"
          udBus.aw.payload.lock := 0
          udBus.aw.payload.cache := 0
          if (config.axiConfig.useQos) {
            udBus.aw.payload.qos := 0
          }
          udBus.aw.payload.prot := 0

          udBus.w.valid := !dataAccepted
          udBus.w.data := command.data
          udBus.w.strb := command.be
          udBus.w.last := True

          when(
            (addressAccepted || udBus.aw.fire) &&
              (dataAccepted || udBus.w.fire)
          ) {
            goto(stateBoot)
          }
        }
      }
    }
}
