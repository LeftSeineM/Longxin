# WB-N4-A DISPATCH-valid fanout control

Status: **accepted as a small PPA checkpoint, not a major performance
release**.

## 1. Difference from the open-source-derived WB-N2 baseline

WB-N2's registered `DISPATCH_arbitration_isValid` signal has more than 500
loads across the issue queues and ROB write controls.  N4-A adds one
FPGA-synthesis attribute to that register:

```verilog
(* max_fanout = "64" *) reg DISPATCH_arbitration_isValid;
```

Vivado may replicate the register to shorten physical routes.  No Boolean
logic, pipeline stage, issue/retire width, predictor, cache or MMU behavior is
changed.  A generated-RTL diff against WB-N2 contains only this attribute plus
the generated date and Git-hash comments.

## 2. Source delta

Compared with tag `WB-N2-B`:

| Item | Delta |
|---|---:|
| Files changed under `src/` | 3 |
| Insertions/deletions | +23 / -1 |
| Changed lines / 8,585 WB-N2 `src` lines | 0.280% |
| Generated functional RTL delta | one synthesis-attribute line |

Most added source lines define a reusable timing configuration and named N4
profile; the hardware intervention itself is the local register attribute.

## 3. Functional and cycle result

| Test | WB-N2 CPU Count | WB-N4-A CPU Count | Result | Cycle delta |
|---|---:|---:|---|---:|
| bitcount | 25,114 | 25,114 | PASS | 0.000% |
| CoreMark | 404,385 | 404,385 | PASS | 0.000% |

All collected commit, cache and branch counters match WB-N2.  Measured CPI/IPC
therefore neither improves nor regresses.

The complete official performance suite was subsequently run in two preserved
segments (14 tests before a user-requested pause, then the remaining 6):
**20/20 PASS**.  The merged baseline has a SoC-count geometric mean of
200,777.78 and a CPU-count geometric mean of 180,049.55.  See
`reports/WB-N4/official/all20_summary.md`.

The full-suite miss profile identifies data-cache behavior as the dominant
remaining bottleneck:

- `fireye_A0`: 3,113,544 CPU cycles and 15,334 D-cache misses;
- `inner_product`: 1,245,307 CPU cycles and 4,407 D-cache misses;
- `loop_induction`: 775,449 CPU cycles and 2,075 D-cache misses;
- `fireye_D1`: 543,646 CPU cycles and 1,897 D-cache misses.

Performance counters include boot/measurement regions that differ from the
official CPU Count interval, so their commit/count ratio is diagnostic and is
not reported as architectural IPC.

## 4. Routed PPA

Same Vivado 2025.2 flow, device and constraints:

| Metric | WB-N2-B | WB-N4-A | Change |
|---|---:|---:|---:|
| Routed LUT | 43,516 | 43,377 | -139 (-0.319%) |
| Routed FF | 16,954 | 17,006 | +52 (+0.307%) |
| BRAM | 28.5 | 28.5 | 0 |
| DSP | 4 | 4 | 0 |
| WNS at 10.00 ns | +0.134 ns | +0.139 ns | +0.005 ns |
| WNS at 9.90 ns | +0.034 ns | +0.039 ns | +0.005 ns |
| WNS at 9.85 ns | -0.016 ns | -0.011 ns | +0.005 ns |
| Delay-derived Fmax | 101.358 MHz | 101.410 MHz | +0.051% |
| Frequency × cycles estimate | 1.0000x | 1.0005x | +0.051% |

The delay-derived Fmax is a routed-checkpoint comparison, not an on-board
stable-clock claim.

## 5. Progress and regression

- Progress: 139 fewer LUTs, 5 ps more routed timing margin, unchanged cycles.
- Regression: 52 additional flip-flops due to deliberate replication.
- Overall: technically positive but too small to present as the main
  performance innovation.  It is retained as a checkpoint while other
  fanout thresholds and architectural optimizations are evaluated.

## 6. Fanout-threshold ablation

Thresholds 128 and 256 were also generated and synthesized.  The 128 build
routed to exactly the same WNS, LUT, FF and critical path as N4-A.  The 256
build produced the same synthesized-netlist checksum as N4-A.  Vivado
therefore converged all three thresholds to the same physical intervention;
64 remains the named default and the redundant profiles are not retained.

Evidence:

- `reports/WB-N4/official/bitcount_summary.md`
- `reports/WB-N4/official/coremark_summary.md`
- `reports/WB-N4/synth/timing_synth_10ns.rpt`
- `reports/WB-N4/route/timing_route_10ns.rpt`
- `reports/WB-N4/route/timing_route_9p9ns.rpt`
- `reports/WB-N4/route/timing_route_9p85ns.rpt`
- `reports/WB-N4/route/utilization_route.rpt`
