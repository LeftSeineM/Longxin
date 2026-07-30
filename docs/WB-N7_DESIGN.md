# WB-N7 checkpointed return-address stack design

Status: **candidate under full validation**.

Baseline: accepted `WB-N6B-20PASS`.

## 1. Goal

WB-N7 replaces the inherited speculative return-address stack (RAS) control
with an independently maintained component while preserving the existing
fetch and ROB checkpoint widths.  The release gate is strict:

- official functional test 58/58;
- official performance test 20/20;
- no CPU-cycle geometric-mean regression;
- routed 100 MHz timing with WNS greater than or equal to zero.

N6 remains an elaboration-time fallback, so the effect of this replacement is
measured without mixing unrelated changes.

## 2. External contract

The RAS receives two classes of events.

At fetch:

- snapshot the current speculative stack pointer for every fetch packet;
- on a predicted call, push the architectural return PC;
- on a predicted return, produce the latest saved return PC and pop.

At commit:

- restore the saved pointer when a predicted branch is no longer a branch;
- on a misprediction, restore the checkpoint and atomically apply the actual
  call, return or ordinary-branch effect.

The checkpoint remains `log2(rasEntries)` bits.  No new field is added to the
fetch buffer, micro-op or ROB, so the replacement does not increase those
wide payloads.

## 3. Independent state model

The inherited implementation uses a pointer to the current top entry:

```text
call:   write stack[top + 1], then top = top + 1
return: read stack[top],     then top = top - 1
```

`WeBattleCheckpointRAS` instead stores a pointer to the next free entry:

```text
call:   write stack[nextFree], then nextFree = nextFree + 1
return: read stack[nextFree-1], then nextFree = nextFree - 1
```

The checkpoint and recovery equations are consequently written from the new
state convention rather than translated copies of the inherited assignments.
All entries have a deterministic reset value, including a return on an empty
logical stack.

## 4. Atomic recovery priority

The inherited plugin assigns the RAS pointer from separate fetch and commit
areas.  Their same-cycle priority is implicit in elaboration order.  N7 has a
single state transition owner and defines the priority explicitly:

```text
commit repair > speculative fetch update > hold
```

A commit redirect invalidates the younger fetch operation, so it must win.
For a mispredicted call, the component restores the checkpoint, writes
`pc + 4`, and advances the pointer in the same transition.  Return and
ordinary-branch repairs are similarly atomic.

## 5. A/B selection

`FrontendConfig.useWeBattleCheckpointRAS` selects the implementation:

- N6: inherited RAS;
- N7: `WeBattleCheckpointRAS`.

`ReturnAddressStackPlugin` remains only the integration/service boundary
required by the existing predictor.  Its inherited implementation is retained
as the explicit false branch until N7 completes all gates.

## 6. Preliminary evidence

The simulation and synthesis RTL variants elaborate successfully.  Official
functional testing passes **58/58**.

Initial performance smoke results:

| Test | N6B CPU cycles | N7 CPU cycles | Change |
|---|---:|---:|---:|
| bitcount | 24,396 | 24,396 | 0.0000% |
| CoreMark | 379,479 | 379,449 | **-0.0079%** |

The CoreMark difference is only 30 cycles and is not yet promoted as a general
performance claim.  The full 20-test run remains the deciding cycle evidence.

N7 has completed physical implementation at the 10 ns constraint:

| Post-route metric | N6B | N7 | Change |
|---|---:|---:|---:|
| LUT | 47,030 | 42,855 | **-4,175 (-8.88%)** |
| FF | 17,009 | 17,036 | +27 (+0.16%) |
| BRAM | 30.5 | 30.5 | 0 |
| DSP | 4 | 4 | 0 |
| WNS | +0.019 ns | **+0.121 ns** | +0.102 ns |
| Delay-derived Fmax | 100.190 MHz | **101.224 MHz** | **+1.03%** |

TNS is zero and the final route has zero routing errors.  Aggressive
post-route physical optimization preserved the same already-clean result.

## 7. Source-difference scope

The N7 increment adds a 68-line independent component and changes the
configuration/integration wrapper.  Relative to N6B, the current increment is
approximately `+136/-11` across four source files.

Text churn is reported only for auditability.  It is not treated as proof of
algorithmic novelty, and the surrounding CPU remains an attributed NOP-Core
derivative.
