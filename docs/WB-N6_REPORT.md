# WB-N6 independent fetch queue report

Status: **accepted engineering checkpoint — all declared simulation and
physical gates are complete**.

Baseline: accepted `WB-N5G-20PASS`.

## 1. Purpose

N6 is not intended to inflate the processor's claimed originality by renaming
NOP-Core files.  It introduces the first completely separate WeBattle
microarchitectural replacement: the four-enqueue, three-dequeue instruction
queue between fetch and decode.

The surrounding CPU remains an explicit NOP-Core derivative.  Component
ownership is disclosed in `docs/WB_COMPONENT_OWNERSHIP.md`.

## 2. Functional and performance status

| Gate | N5G | N6A | N6B |
|---|---:|---:|---:|
| Scala/Spinal RTL generation | PASS | PASS | PASS |
| Official functional | 58/58 | 58/58 | **58/58** |
| bitcount CPU cycles | 24,396 | 24,396 | 24,396 |
| CoreMark CPU cycles | 379,479 | 379,479 | 379,479 |
| Official performance | 20/20 | **20/20** | **20/20** |
| 100 MHz route | PASS | FAIL (-0.170 ns) | **PASS (+0.019 ns)** |

N6A's full result is cycle- and counter-identical to N5G: CPU and SoC
geometric-mean speedups are both **1.000000x (0.000%)**.

N6B's full result is also cycle- and counter-identical to N5G:

| Performance metric | N5G | N6B | Change |
|---|---:|---:|---:|
| CPU-cycle geometric mean | 175,018.46 | 175,018.46 | **0.000%** |
| SoC-cycle geometric mean | 197,295.19 | 197,295.19 | **0.000%** |
| CPU-cycle speedup | 1.000000x | 1.000000x | **0.000%** |
| Total branch mispredictions | 58,433 | 58,433 | **0.000%** |
| Delay-derived Fmax | 100.100 MHz | 100.190 MHz | **+0.090%** |
| `Fmax / CPU-cycle geomean` | 1.000000 | 1.000902 | **+0.090%** |

N6A and N6B have identical queue state semantics.  N6B replaces only the
wide dynamic storage index with an explicit one-hot circular slot selector.
The full N6B gates are nevertheless rerun rather than inferred.

## 3. N6A PPA ablation

| Metric | N5G | N6A direct index | Change |
|---|---:|---:|---:|
| Synth LUT | 44,358 | 47,763 | +3,405 (**+7.676%**) |
| Synth FF | 16,994 | 16,990 | -4 (**-0.024%**) |
| Route LUT | 43,857 | 47,208 | +3,351 (**+7.641%**) |
| Route FF | 17,016 | 17,011 | -5 (**-0.029%**) |
| Route BRAM | 30.5 | 30.5 | 0 |
| Route DSP | 4 | 4 | 0 |
| Route WNS at 10 ns | +0.010 ns | **-0.170 ns** | -0.180 ns; FAIL |

N6A is functionally and cycle correct but rejected as the release organization
because directly indexing a wide, four-write register vector replicates
address logic through the payload.  Its route has zero routing errors, so the
negative WNS is a completed physical result.  N6A is retained only as an
implementation ablation.

N6B hierarchy analysis shows that its queue instance is smaller than the
inherited instance (2,497 versus 2,854 LUT, **-12.509%**), but the standalone
component boundary increases parent-level logic enough to make total synth LUT
47,547 (**+7.189%** versus N5G).  N6C therefore inlines the same independently
maintained implementation as a Spinal `Area` to restore cross-boundary
optimization.

N6C failed its first correctness gate: bitcount was still running at 59 ms
instead of finishing near 0.276 ms.  The directionless inline streams did not
preserve the directional component handshake.  Its simulation and physical
run were stopped, and the source was restored to N6B.  N6C is a rejected
correctness ablation, not a release result.

N6B final physical result:

| Route metric | N5G | N6B | Change |
|---|---:|---:|---:|
| LUT | 43,857 | 47,030 | +3,173 (**+7.235%**) |
| FF | 17,016 | 17,009 | -7 (**-0.041%**) |
| BRAM | 30.5 | 30.5 | 0 |
| DSP | 4 | 4 | 0 |
| WNS at 10 ns | +0.010 ns | **+0.019 ns** | +0.009 ns |
| Delay-derived Fmax | 100.100 MHz | **100.190 MHz** | **+0.090%** |

TNS is zero and the final route has zero routing errors.  The independent
replacement therefore preserves the 100 MHz timing gate, but its 7.235% LUT
cost remains a real disclosed regression.

## 4. Source difference

Against N5G under `src/`:

| Item | Difference |
|---|---:|
| Files changed/added | 4 |
| Insertions / deletions | +158 / -21 |
| Churn / 8,694 N5 nonblank source lines | **2.059%** |
| Net source growth | 137 lines (**1.576%**) |
| New independent queue implementation | 109 physical lines |

Against the frozen local NOP-Core `src` tree:

| Item | Difference |
|---|---:|
| Files changed/added | 12 |
| Insertions / deletions | +610 / -78 |
| Churn / 8,336 upstream nonblank source lines | **8.253%** |
| Net source growth | 532 lines (**6.382%**) |

These numbers measure text change, not novelty.  After removing comments,
blank lines, package names and imports, the N6B queue and inherited FIFO main
class have six exactly shared unique lines, about **8.7%** of the normalized N6
queue.  Those lines are generic component/interface boilerplate.

## 5. Design differences from the inherited queue

| Topic | Inherited `MultiPortFIFOVec` | `WeBattleFetchQueue` |
|---|---|---|
| Full/empty state | equal pointers + phase/direction bit | explicit 0..8 occupancy |
| Enqueue/dequeue accounting | pointer-distance and phase inference | first non-fire dense count |
| Flush | wrapper reaches into FIFO state | native atomic queue input |
| Storage selection | helper functions around phase FIFO | one-hot circular slot selector |
| Contract checking | comments | simulation assertions |
| Fallback | only implementation | N5/N6 elaboration-time A/B switch |

Explicit occupancy is not presented as a novel FIFO algorithm.  The
competition contribution is the independent implementation, integration,
measured FPGA trade-off and preserved fallback.

## 6. Current limitations

- N6 replaces one frontend component; ROB, rename, issue queues, caches and
  execution units are still inherited and disclosed as such.
- A component replacement that damages WNS or official performance will remain
  an experiment rather than silently replacing N5G.
- Board stability and Linux are outside this checkpoint.

## 7. Final evidence

- N6B functional 58/58:
  `reports/WB-N6B/official/func58_summary.md`
- N6B performance 20/20:
  `reports/WB-N6B/official/all20_summary.md`
- N6B versus N5G per-test comparison:
  `reports/WB-N6B/official/n6b_vs_n5g.md`
- N6B final timing:
  `reports/WB-N6B/implementation/timing_route_post_physopt_10ns.rpt`
- N6B final utilization:
  `reports/WB-N6B/implementation/utilization_route_post_physopt.rpt`
- N6B final route status:
  `reports/WB-N6B/implementation/route_status_post_physopt.rpt`
- N6A rejected physical ablation:
  `reports/WB-N6/implementation/timing_route_10ns.rpt`
