# WB-N10 timing-safe integer issue-queue replacement

Status: **accepted performance and ownership checkpoint**.

Baseline: accepted `WB-N9-20PASS`.

## 1. Accepted scope

N10 replaces the inherited integer reservation-station state transition with
`WeBattleDenseIssueQueueState`. The accepted boundary owns:

1. completion and three-lane speculative wakeup folding;
2. dense dispatch acceptance and registered-capacity backpressure;
3. up to three mutually exclusive issue removals;
4. bounded survivor compaction and ordered append;
5. atomic highest-priority flush.

The inherited integer queue boundary has a frozen weight of 96 nonblank lines.
Accepted component-weighted ownership therefore becomes:

`(988 + 96) / 4,791 = 22.63%`

The generic state engine is also integrated behind elaboration switches for
the memory and MulDiv queues. `WeBattleMulDivUnit` implements independent MDU
control around the retained Xilinx multiplier and Spinal divider arithmetic
primitives. Those two boundaries remain experimental and receive no accepted
ownership credit in N10.

## 2. State and timing structure

Live entries occupy a dense prefix. Dispatch lanes are appended in order using
registered occupancy and never borrow same-cycle issue capacity. A producer
completion can wake either a resident entry or an entry accepted on the same
edge. Flush overrides all normal state transitions.

The first implementation rebuilt every destination from every possible
survivor rank. It was functionally correct but physically expensive. The
accepted implementation exploits the three-issue bound: a destination selects
only itself or one of the following three slots. Payload compaction is resolved
before completion tags are applied at the final destination slot. This removes
the original `issue select -> wakeup -> compaction -> operand valid` serial
cone without adding a pipeline stage.

## 3. Rejected N10A full-cluster experiment

N10A enabled the integer, memory and MulDiv queue replacements together with
the new MDU controller. After the MDU command-capture bubble was removed, it
passed official functionality 58/58 and performance 20/20. It was still
rejected because routed timing failed:

| Candidate | WNS | TNS | Result |
|---|---:|---:|---|
| N10A initial route | -0.912 ns | -132.752 ns | reject |
| N10A aggressive post-route | -0.359 ns | -5.676 ns | reject |

N10A remains an explicit source configuration for further physical redesign;
its 30.16% planned coverage is not claimed.

## 4. Accepted validation

| Gate or metric | N10 |
|---|---:|
| Scala compile / Spinal elaboration | PASS |
| Official functionality | **58/58 PASS** |
| Official performance | **20/20 PASS** |
| CPU-cycle geometric mean | **172,808.44** |
| SoC-cycle geometric mean | **194,705.69** |
| CPU-cycle speedup versus N9 | **1.001096x (+0.110%)** |
| CPU-cycle speedup versus N7 | **1.012678x (+1.268%)** |
| Routed WNS / TNS | **+0.036 ns / 0** |
| Route errors | **0** |
| Route-derived Fmax | **100.361 MHz** |
| LUT / FF | **45,385 / 20,347** |
| BRAM / DSP | **30.5 / 4** |
| Combined Fmax/cycle versus N9 | **1.004512x (+0.451%)** |
| Combined Fmax/cycle versus N7 | **1.004039x (+0.404%)** |

N10 is both the accepted ownership head and the new strict combined
performance head.

## 5. Next versions

The remaining scheduling work is deliberately split:

1. N11: MulDiv queue and MDU controller, 242 weighted lines;
2. N12: memory issue queue, 119 weighted lines;
3. N13: LSU/control cluster;
4. N14: instruction front-end control cluster.

Each version must independently pass 58/58, 20/20 and non-negative routed WNS.
