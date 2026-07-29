# WeBattle-Core component ownership matrix

This document separates inherited NOP-Core work from WeBattle-maintained and
independently implemented work.  It is an engineering and competition
disclosure, not a claim that textual difference alone establishes originality.

Upstream base:

- project: NOP-Core;
- commit: `1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`;
- license: MIT;
- all inherited copyright and license notices remain in the repository.

## 1. Current N6 ownership

| Area | N6 status | WeBattle contribution | Evidence |
|---|---|---|---|
| 2026 SoC top/wrapper | WeBattle-maintained | Narrow contest-native top, CPUCFG compatibility and event ports | `src/WeBattleRawTop.scala`, `integration/core_top.sv` |
| Address-translation profile | WeBattle specialization | Compile-time preliminary-round DA/DMW path with full MMU fallback | `docs/WB-N2_INNOVATION.md` |
| Branch direction prediction | Substantially modified inherited frontend | Added packed global+bimodal tournament chooser, snapshot transport and training | `docs/WB-N5_DESIGN.md` |
| Fetch instruction queue | **Independent N6 replacement** | Explicit-occupancy 4-enqueue/3-dequeue queue, dense-lane contract, atomic flush and assertions | `src/pipeline/fetch/WeBattleFetchQueue.scala` |
| Program counter and I-cache | Inherited | Configuration/integration only | NOP-Core source |
| BTB and RAS | Inherited with predictor integration | No full replacement claimed yet | NOP-Core source |
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
