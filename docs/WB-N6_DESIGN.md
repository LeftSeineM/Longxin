# WB-N6 independent fetch queue design

Status: **accepted engineering checkpoint**; N6B passes 58/58 functional,
20/20 performance and the 100 MHz physical timing gate.

## 1. Goal

WB-N6 keeps the accepted N5G packed tournament predictor and replaces the
inherited `MultiPortFIFOVec` used by the fetch buffer with
`WeBattleFetchQueue`, an independently implemented frontend component.

This version is primarily an ownership and maintainability checkpoint.  Its
promotion goal is no performance regression; any PPA change is measured rather
than assumed beneficial.

## 2. Interface contract

The processor fetches up to four instructions and decodes up to three each
cycle.  The queue therefore provides:

- four ordered enqueue streams;
- three ordered dequeue streams;
- eight entries of register-backed storage;
- atomic pipeline-flush recovery;
- no same-cycle enqueue-to-dequeue bypass;
- no use of capacity freed by dequeue until the following cycle.

Valid enqueue lanes and ready dequeue lanes must be dense from lane zero.
These rules match the frozen N5 frontend timing boundary and prevent a new
combinational I-cache-to-decode path.

## 3. Independent state model

The inherited FIFO distinguishes empty from full using equal read/write
pointers plus a direction/phase bit.  N6 instead maintains:

```text
head       next instruction to dequeue
tail       next entry to enqueue
occupancy  exact number of live entries, from 0 through 8
```

Availability and validity are direct comparisons against `occupancy`.
Enqueue/dequeue counts are determined by the first non-firing dense lane.
On a flush, `head`, `tail` and `occupancy` reset together with priority over
normal updates.

The implementation includes simulation assertions for:

- occupancy never exceeding depth;
- dense enqueue valid lanes;
- dense dequeue ready lanes.

## 4. A/B fallback

`FrontendConfig.useWeBattleFetchQueue` selects the implementation at
elaboration time:

- N5: inherited queue;
- N6: WeBattle queue.

The rest of the frontend receives the same `Stream[InstBufferEntry]` interface.
This permits exact cycle and implementation comparisons without maintaining
two processor branches.

## 5. Current validation

Scala compilation, SpinalHDL elaboration and simulation/synthesis Verilog
generation pass.

Both N6A and the optimized N6B official functional replays pass **58/58** with
zero failed points and no queue-contract assertion failure.  N6A also passes
the complete performance suite **20/20**; every cycle and recorded event
counter is identical to N5G.  N6A is still rejected because its physical
result fails.

Initial official performance smoke tests are cycle-identical to N5G:

| Test | N5G CPU cycles | N6 CPU cycles | Change |
|---|---:|---:|---:|
| bitcount | 24,396 | 24,396 | 0.000% |
| CoreMark | 379,479 | 379,479 | 0.000% |

Neither the queue contract assertions nor the official testbench reported a
failure.  These two tests do not promote N6 by themselves; 58/58, 20/20 and
post-route timing remain required.

### N6A storage-access ablation

The first functionally correct implementation indexed the wide register
storage directly with `tail + lane` and `head + lane`.  Although its test
cycles were identical, synthesis expanded the dynamic multiwrite path:

| Metric | N5G | N6A direct index | Change |
|---|---:|---:|---:|
| Synth LUT | 44,358 | 47,763 | +3,405 (**+7.676%**) |
| Synth FF | 16,994 | 16,990 | -4 (**-0.024%**) |
| BRAM | 30.5 | 30.5 | 0 |
| DSP | 4 | 4 | 0 |

N6A is therefore an ablation, not the release candidate.  The optimized N6B
uses explicit one-hot circular slot selection so the small pointer decoder is
shared rather than replicated across the wide payload.  Its occupancy state
model and interface contract are unchanged.

N6B has repeated Scala/Spinal generation and the two-test performance smoke:

| Test | N5G | N6A | N6B | N6B vs N5G |
|---|---:|---:|---:|---:|
| bitcount CPU cycles | 24,396 | 24,396 | 24,396 | 0.000% |
| CoreMark CPU cycles | 379,479 | 379,479 | 379,479 | 0.000% |

N6B subsequently completed the full official performance suite **20/20**.
Every CPU cycle, SoC cycle and recorded event counter is identical to N5G.
An independent duplicate run of the last four programs also matches every
field.  No queue assertion fired.

N6A also completed route:

| Route metric | N5G | N6A direct index | Change |
|---|---:|---:|---:|
| LUT | 43,857 | 47,208 | +3,351 (**+7.641%**) |
| FF | 17,016 | 17,011 | -5 (**-0.029%**) |
| BRAM | 30.5 | 30.5 | 0 |
| DSP | 4 | 4 | 0 |
| WNS at 10 ns | +0.010 ns | **-0.170 ns** | -0.180 ns; FAIL |

The route has zero routing errors, so this is a real PPA/timing rejection
rather than an incomplete-tool result.

### N6B hierarchy ablation and N6C

The N6B one-hot selector reduced the queue instance itself:

| Synth hierarchy | N5G inherited queue | N6B WeBattle queue | Change |
|---|---:|---:|---:|
| Queue LUT | 2,854 | 2,497 | **-357 (-12.509%)** |
| Queue FF | 950 | 937 | **-13 (-1.368%)** |

However, making the queue a separate named Spinal `Component` changed
cross-boundary optimization.  Unattributed parent CPU logic increased enough
that total synth LUT was 47,547, still 7.189% above N5G.  The local queue was
better but the chip-level result was not.

N6C keeps the same explicit-occupancy and one-hot-ring implementation but
elaborates it as an inline `Area`.  This preserves an independently maintained
source module while allowing Vivado to optimize across the fetch-buffer
boundary.

N6C was rejected immediately by the first official bitcount run: it had not
completed by 59 ms, versus 0.276 ms for the correct N6B build.  Converting
directional component I/O to internal directionless streams changed the
fetch/decode handshake despite successful elaboration.  Its physical run was
stopped because correctness is the first gate.  The source is restored to the
N6B directional `Component`; N6C is not part of the candidate.

N6B completed post-route physical optimization:

| Final route metric | N5G | N6B | Change |
|---|---:|---:|---:|
| LUT | 43,857 | 47,030 | +3,173 (**+7.235%**) |
| FF | 17,016 | 17,009 | -7 (**-0.041%**) |
| BRAM | 30.5 | 30.5 | 0 |
| DSP | 4 | 4 | 0 |
| WNS at 10 ns | +0.010 ns | **+0.019 ns** | +0.009 ns |
| Delay-derived Fmax | 100.100 MHz | **100.190 MHz** | **+0.090%** |

TNS is zero and the route-status report has zero routing errors.  The LUT cost
is disclosed rather than hidden: N6B trades 7.235% more LUT for a fully
independent fetch-queue implementation while preserving the 100 MHz gate.

## 6. Attribution

The surrounding fetch/decode pipeline and `InstBufferEntry` format are
inherited from NOP-Core.  `WeBattleFetchQueue.scala` is a new implementation
written for this repository from the interface contract above.  It does not
copy the control structure or phase-bit state model of `MultiPortFIFOVec`.
