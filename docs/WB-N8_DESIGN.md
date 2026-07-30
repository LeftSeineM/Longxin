# WB-N8 integer issue/execute cluster replacement

Status: **validated as part of the accepted WB-N9 packaged checkpoint**.
N8 was intentionally not promoted as a separate release; its integer
issue/execute cluster is included in the N9 58/58, 20/20 and routed-timing
results.

Baseline: N7 candidate, which itself is derived from accepted
`WB-N6B-20PASS`.

## 1. Why N8 is a cluster, not a tiny checkpoint

The first N8 draft replaced only the 3-port/7-slot issue selector.  That is a
real hardware boundary but too small to justify a separately promoted contest
version or a complete official regression run.

N8 is therefore defined as one coherent integer-backend replacement bundle:

1. age-ordered, mutually exclusive multi-port issue selection;
2. physical-register completion wakeup matrix;
3. integer ALU result datapath;
4. branch comparison;
5. direct/indirect branch target generation;
6. actual-taken and misprediction decision.

The issue queue storage, compression and wakeup bits remain inherited in this
checkpoint and are disclosed as such.  They are not counted as replaced.

## 2. Age-ordered issue selector

Inputs are one request bitmap per integer execution port.  Slot zero is the
oldest queue entry, and port zero has the highest assignment priority.

The inherited generic selector repeatedly applies `OHMasking.first` and masks
earlier grants from later ports.  `WeBattleAgeOrderedIssueSelector` instead
builds an explicit prefix network:

```text
eligible[p] = request[p] & ~allocatedByEarlierPorts
grant[p,s]  = eligible[p,s] & ~any(eligible[p,0 .. s-1])
allocated  |= grant[p]
```

The output is one-hot per port and mutually exclusive across ports without a
binary encode/decode round trip.  The logic is elaborated as an inline
Spinal `Area`, preserving cross-boundary FPGA optimization.

## 3. Completion wakeup matrix

The global wakeup path is also replaced.  `WeBattleWakeupMatrix` compares
every reservation-station operand tag against all physical-register completion
broadcasts in parallel and emits one wake bit per operand.  Queue state updates
remain in the issue-queue owner, avoiding a second writer or a new cycle of
latency.  This pure combinational matrix is also inline; the existing local
speculative wakeup/rollback mechanism is retained.

## 4. Integer ALU

`WeBattleIntegerALU` groups operations by physical result class:

- add/subtract and architectural aliases;
- AND/OR/XOR/NOR;
- signed/unsigned set-less-than;
- logical/arithmetic shifts;
- CPUCFG compatibility result.

Signed set-less-than is expressed as unsigned comparison after flipping each
operand's sign bit.  Architectural aliases share one arithmetic path
explicitly.  The final operation selection occurs once at the component
output.

## 5. Combined branch execute block

The inherited backend instantiates separate comparator and branch-resolution
components.  `WeBattleBranchExecute` combines:

- all signed, unsigned and zero comparison predicates;
- conditional, direct and indirect target generation;
- one shared `actualTaken` term;
- direction and target mismatch detection.

The block keeps the existing EXE-stage boundary, so it adds no pipeline stage
and does not change branch recovery latency.

## 6. A/B fallback

Three elaboration flags are carried in `IntIssueConfig`:

- `useWeBattleAgeSelector`;
- `useWeBattleWakeupMatrix`;
- `useWeBattleIntegerDatapath`.

N7 leaves all three false.  N8 enables all three.  The inherited selector, ALU,
comparator and BRU remain compiled fallback branches until all official and
physical gates pass.

## 7. Current source scope

The independent N8 cluster currently contains:

| Component | Physical source lines |
|---|---:|
| `WeBattleAgeOrderedIssueSelector` | 32 |
| `WeBattleWakeupMatrix` | 34 |
| `WeBattleIntegerALU` | 70 |
| `WeBattleBranchExecute` | 79 |
| Independent component subtotal | **215** |

Integration changes also restructure the integer EXE selection path and add
configuration fallbacks.  N8 is not promoted merely because it is larger:
functional behavior, complete performance results and routed timing decide
whether it survives.

## 8. Validation plan

N8 will not consume a full regression slot until N7's currently running suite
finishes.  The gates are:

1. simulation and synthesis RTL elaboration;
2. official bitcount/CoreMark smoke;
3. official functional 58/58;
4. official performance 20/20;
5. post-route timing at 100 MHz;
6. cycle, event-counter, LUT/FF/BRAM/DSP and WNS comparison against N7/N6B.

Any functional failure is fixed before performance testing.  A cycle or WNS
regression remains an experiment rather than being silently promoted.
