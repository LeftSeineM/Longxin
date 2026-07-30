# WB-N11 MulDiv scheduling and execution replacement

Status: **accepted ownership checkpoint**.

Baseline: `WB-N10-20PASS`.

## Accepted boundary

N11 enables the WeBattle dense state engine for the three-entry MulDiv queue
and replaces inherited MulDiv execution control with `WeBattleMulDivUnit`.
The accepted logic owns:

- command capture and single-accept lifetime;
- multiply/divide operation selection;
- signed-magnitude preprocessing and result correction;
- 16-bit early-divider selection;
- response/flush handshake and result selection;
- one-cycle-early multiply dependency wakeup;
- MulDiv queue append, issue removal, compaction, wakeup and flush.

The licensed Xilinx two-cycle multiplier and Spinal unsigned-divider arithmetic
primitives remain reused and attributed. N11 claims the surrounding scheduling
and control implementation, not those arithmetic primitives.

## Removed performance bubble

The first command wrapper registered operands before driving the multiplier,
adding one cycle to every multiply. CoreMark regressed 2.78% in the isolated
ablation. The accepted unit drives incoming magnitudes to the multiplier on
the command-accept edge, counts that edge as multiplier pipeline cycle one,
and restores the inherited one-cycle-early wakeup contract.

After the fix, N11 and N10 are cycle-identical on 19 of 20 official tests.
`fireye_I2` adds 400 CPU cycles; the CPU geometric-mean regression is only
0.0073%.

## Validation

| Gate or metric | N11 |
|---|---:|
| Official functionality | **58/58 PASS** |
| Official performance | **20/20 PASS** |
| CPU-cycle geometric mean | **172,821.02** |
| SoC-cycle geometric mean | **194,719.81** |
| Routed WNS / TNS | **+0.009 ns / 0** |
| Route errors | **0** |
| Route-derived Fmax | **100.090 MHz** |
| LUT / FF | **45,562 / 20,495** |
| BRAM / DSP | **30.5 / 4** |
| Combined score versus N10 | **0.997225x (-0.278%)** |
| Combined score versus N9 | **1.001724x (+0.172%)** |
| Combined score versus N7 | **1.001253x (+0.125%)** |

N11 is the accepted ownership head. N10 remains the strict performance head.

## Ownership

The frozen MulDiv queue/execute boundary weight is 242 lines:

`(1,084 + 242) / 4,791 = 27.68%`

The memory issue queue remains disabled in N11 and receives no ownership
credit until N12 passes its own gates.
