# WB-N7 checkpointed RAS final report

Status: **accepted engineering checkpoint**.

Baseline: `WB-N6B-20PASS`.

## 1. Result

N7 replaces the inherited return-address stack control with the independent
`WeBattleCheckpointRAS` and passes every declared promotion gate:

| Gate | Result |
|---|---:|
| Simulation/synthesis RTL generation | PASS |
| Official functional | **58/58** |
| Official performance | **20/20** |
| Routed 100 MHz WNS | **+0.121 ns** |
| Routed TNS | **0.000 ns** |
| Routing errors | **0** |

Aggressive post-route physical optimization retained the already timing-clean
route without changing its metrics.

## 2. Performance versus N6B

| Metric | N6B | N7 | Change |
|---|---:|---:|---:|
| CPU-cycle geometric mean | 175,018.46 | **174,999.32** | **-0.0109%** |
| SoC-cycle geometric mean | 197,295.19 | **197,280.70** | **-0.0073%** |
| CPU-cycle speedup | 1.000000x | **1.000109x** | +0.0109% |
| Tests faster / tied / slower | - | **2 / 18 / 0** | no regression |

The only CPU-cycle changes are:

| Test | N6B | N7 | Reduction |
|---|---:|---:|---:|
| CoreMark | 379,479 | **379,449** | 30 (0.0079%) |
| stringsearch | 31,808 | **31,741** | 67 (0.2106%) |

The effect is small and is not described as a major IPC improvement.  It is
consistent with making commit repair win atomically over a same-cycle younger
speculative RAS update.  Some diagnostic branch counters also change because
the wrong-path instruction mix changes; the official CPU-cycle result is the
promotion metric.

## 3. Physical result

| Post-route metric | N6B | N7 | Change |
|---|---:|---:|---:|
| LUT | 47,030 | **42,855** | **-4,175 (-8.88%)** |
| FF | 17,009 | 17,036 | +27 (+0.16%) |
| BRAM | 30.5 | 30.5 | 0 |
| DSP | 4 | 4 | 0 |
| WNS at 10 ns | +0.019 ns | **+0.121 ns** | +0.102 ns |
| Delay-derived Fmax | 100.190 MHz | **101.224 MHz** | **+1.03%** |

The large LUT reduction is a whole-core post-route measurement, not the size
of the RAS alone.  The next-free pointer convention removes the inherited
`top + 1` dynamic write address and gives all RAS state one explicit transition
owner; synthesis and hierarchy rebuilding consequently find a materially
smaller whole-core implementation.  This is an evidence-backed observation,
not proof that every 4,175-LUT difference is located inside the RAS instance.

Combining delay-derived Fmax and CPU-cycle geometric mean gives an estimated
throughput ratio of approximately **1.0104x N6B (+1.04%)**.

## 4. Implementation difference

N7 adds a 68-line independent component.  Relative to N6B under `src/`, the
increment is approximately:

| Source metric | N7 versus N6B |
|---|---:|
| Files changed/added | 4 |
| Insertions / deletions | **+136 / -11** |
| Churn | 147 lines |

The architectural differences are:

- pointer denotes the next free slot instead of the current top;
- call writes `stack[nextFree]` instead of `stack[top + 1]`;
- return reads `stack[nextFree - 1]`;
- fetch and commit no longer write RAS state from separate owners;
- commit recovery has explicit priority over younger speculative update;
- mispredicted call/return repair is one atomic transition;
- existing checkpoint width and fetch/ROB payloads remain unchanged;
- N6 remains an elaboration-time fallback.

Text churn is disclosed for auditing only.  The surrounding processor remains
an attributed NOP-Core derivative.

## 5. Evidence

- design contract: `docs/WB-N7_DESIGN.md`;
- functional log: `reports/WB-N7/official/func58.log`;
- merged 20-test result: `reports/WB-N7/official/all20_summary.md`;
- per-test N6B comparison: `reports/WB-N7/official/n7_vs_n6b.md`;
- final timing: `reports/WB-N7/implementation/timing_route_post_physopt_10ns.rpt`;
- final utilization:
  `reports/WB-N7/implementation/utilization_route_post_physopt.rpt`;
- final route status:
  `reports/WB-N7/implementation/route_status_post_physopt.rpt`.

## 6. Remaining limitations

- BTB, queue storage/compression, rename, ROB, caches and most backend control
  remain inherited unless separately identified in the ownership matrix.
- N7 is a contest-profile checkpoint, not a Linux/board-stability claim.
- N8 integer issue/execute replacements are evaluated separately and cannot
  retroactively change N7's measured result.
