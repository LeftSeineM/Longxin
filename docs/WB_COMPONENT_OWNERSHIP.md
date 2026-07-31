# WeBattle-Core component ownership matrix

This document separates inherited NOP-Core work from WeBattle-maintained and
independently implemented work.  It is an engineering and competition
disclosure, not a claim that textual difference alone establishes originality.

Upstream base:

- project: NOP-Core;
- commit: `1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`;
- license: MIT;
- all inherited copyright and license notices remain in the repository.

## 1. Current N7 candidate ownership

| Area | N7 status | WeBattle contribution | Evidence |
|---|---|---|---|
| 2026 SoC top/wrapper | WeBattle-maintained | Narrow contest-native top, CPUCFG compatibility and event ports | `src/WeBattleRawTop.scala`, `integration/core_top.sv` |
| Address-translation profile | WeBattle specialization | Compile-time preliminary-round DA/DMW path with full MMU fallback | `docs/WB-N2_INNOVATION.md` |
| Branch direction prediction | Substantially modified inherited frontend | Added packed global+bimodal tournament chooser, snapshot transport and training | `docs/WB-N5_DESIGN.md` |
| Fetch instruction queue | **Independent N6 replacement** | Explicit-occupancy 4-enqueue/3-dequeue queue, dense-lane contract, atomic flush and assertions | `src/pipeline/fetch/WeBattleFetchQueue.scala` |
| Program counter and I-cache | Inherited | Configuration/integration only | NOP-Core source |
| BTB | Inherited with predictor integration | No full replacement claimed yet | NOP-Core source |
| Return-address stack | **Independent N7 candidate replacement** | Next-free pointer state model, unchanged-width per-fetch checkpoints and explicit atomic commit-repair priority | `src/pipeline/fetch/WeBattleCheckpointRAS.scala`, `docs/WB-N7_DESIGN.md` |
| Decode and instruction parser | Inherited | No full replacement claimed | NOP-Core source |
| Rename/RAT/physical register file | Inherited | No full replacement claimed | NOP-Core source |
| Integer/MulDiv/LSU issue queues | Inherited | No full replacement claimed | NOP-Core source |
| ALU/BRU/MulDiv execution | Inherited | No full replacement claimed | NOP-Core source |
| ROB and in-order commit | Inherited | Counters and integration changes only | NOP-Core source |
| I-cache/D-cache/store buffer | Inherited | Rejected experiments are documented, not promoted | `docs/WB-N5_DESIGN.md` |
| CSR, exception, interrupt, MMU/TLB | Inherited | 2026 compatibility and profile selection only | NOP-Core source |
| AXI fabric and buffers | Inherited | No full replacement claimed | NOP-Core source |

## 2. Replacement policy

A component is labelled an independent WeBattle replacement only when all of
the following are true:

1. its behavior and interface contract are documented before promotion;
2. its implementation is written from that contract rather than mechanically
   renaming or reformatting upstream source;
3. the inherited implementation remains available as an A/B fallback until
   validation is complete;
4. official functional, performance and physical results are recorded;
5. regressions and resource costs are disclosed;
6. upstream lineage of the surrounding processor remains explicit.

Concepts such as FIFO occupancy counters, tournament prediction, scoreboards,
ROB structures and cache policies are standard computer-architecture ideas.
Using a standard concept is not by itself claimed as novel.  The defensible
competition contribution is the team's concrete implementation, integration,
trade-off and measured result.

## 3. Planned replacement order

The order is chosen by interface clarity and rollback risk:

1. N6 fetch queue;
2. checkpointed return-address stack;
3. integer issue wakeup/select block;
4. ROB state storage and retirement control;
5. store buffer/load-store dependency handling;
6. selected cache control paths.

Each item receives its own version checkpoint.  A later module is not allowed
to obscure the measured effect of an earlier replacement.

## 4. N8 work-in-progress disclosure

N8 is not yet an accepted checkpoint.  Its independent candidate scope is the
integer age selector plus ALU/comparison/branch-resolution datapath documented
in `docs/WB-N8_DESIGN.md`.  Integer queue storage/compression/wakeup,
MulDiv/LSU issue and MulDiv execution remain inherited.  The table above stays
at the last validated N7 ownership state until N8 passes every gate.

## 5. N9 accepted ownership disclosure

N9 is a single backend-state replacement batch covering rename/FreeList,
physical-register state, ROB storage/completion, and commit/recovery control.
It also validates the N8 integer selector, wakeup and execute replacements as
part of one packaged RTL image. Its contract, audit weights and complete
validation are recorded in `docs/WB-N9_DESIGN.md`.

N9 passed official 58/58 functionality, official 20/20 performance and routed
timing at WNS +0.002 ns. The accepted component-weighted coverage is
988 / 4,791 = **20.62%**. This makes N9 the ownership head. N7 remains the
strict combined performance head because N9's +1.157% cycle speedup is offset
by a -1.190% route-derived Fmax change.

## 6. N10 accepted ownership disclosure

N10 replaces the integer issue-queue state, compaction, append, wakeup folding
and flush boundary with `WeBattleDenseIssueQueueState`. It passes official
58/58 functionality, official 20/20 performance and routed timing at WNS
+0.036 ns. Accepted component-weighted coverage is:

`(988 + 96) / 4,791 = 22.63%`

N10 is also the strict combined performance head: the route-derived
Fmax/cycle estimate is 0.404% above N7. The memory/MulDiv queue integration and
`WeBattleMulDivUnit` are present as attributed N10A experiments but are not
counted until their own later checkpoints pass non-negative routed timing.

## 7. N11 accepted ownership disclosure

N11 accepts the MulDiv issue queue and WeBattle-owned MulDiv command,
signedness, early-divider, response and wakeup controller. The Xilinx
multiplier IP and Spinal unsigned-divider arithmetic primitives remain reused
and attributed.

N11 passes 58/58 functionality, 20/20 performance and routed timing at WNS
+0.009 ns. Its accepted component-weighted coverage is:

`(1,084 + 242) / 4,791 = 27.68%`

N11 is the ownership head. N10 remains the strict performance head because
N11's combined route-derived score is 0.278% lower.

## 8. N12 accepted ownership disclosure

N12 replaces memory issue-queue state, dense append, oldest-entry removal,
one-slot compaction, operand wakeup folding and atomic flush with
`WeBattleMemIssueQueueState`. The surrounding LSU, caches, address-generation
datapath and physical-register broadcast sources remain inherited and are not
counted.

N12 passes 58/58 functionality, 20/20 performance and routed timing at WNS
+0.010 ns. All 20 official cycle counts are exactly equal to N11; routed LUT
use decreases by 3.50%. Its accepted component-weighted coverage is:

`(1,326 + 119) / 4,791 = 30.16%`

N12 is the ownership head and is 0.010% above N11 by the combined routed
estimate. N10 remains the strict performance head because N12 is 0.268% below
it.

## 9. N13 accepted ownership disclosure

N13 replaces five coherent LSU/perimeter boundaries: effective-address and
store-alignment generation, memory issue/execute and completion control,
store-buffer state and forwarding, load extraction/extension, and uncached
store request/response control. The D-cache arrays, lookup, refill and write
machinery remain inherited and are not counted.

N13 passes 58/58 functionality, 20/20 performance and routed timing at WNS
+0.020 ns. All official cycle counts are exactly equal to N12 and routed WNS
improves by 0.010 ns. Its accepted component-weighted coverage is:

`(1,445 + 515) / 4,791 = 40.91%`

N13 is the ownership head and is 0.100% above N12 by the combined routed
estimate. N10 remains the strict performance head because N13 is 0.168% below
it. The project continues to disclose its NOP-Core lineage and does not treat
raw textual churn as proof of originality.

## 10. N14 accepted ownership disclosure

N14 replaces the instruction-cache hit/refill/invalidate controller and the
complete BTB/global+tournament direction-prediction/history-control boundary.
Standard RAM primitives remain reused infrastructure; all request, arbitration,
state-transition, update and recovery control around them is maintained in the
two WeBattle front-end modules.

N14 passes 58/58 functionality, 20/20 performance and routed timing at WNS
+0.006 ns. Every official cycle and diagnostic counter is exactly equal to
N13. Its accepted component-weighted coverage is:

`(1,960 + 253 + 215) / 4,791 = 50.68%`

N14 is the first accepted checkpoint above the 50% component-ownership target.
It uses 580 more LUTs and 17 more flip-flops than N13 and its combined
route-derived estimate is 0.140% lower because routed WNS is 0.014 ns smaller.
N10 remains the strict performance head. N14 remains an attributed NOP-Core
derivative; the 50.68% figure is not a claim that the whole processor was
written from scratch.
