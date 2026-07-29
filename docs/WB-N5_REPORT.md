# WB-N5G packed tournament predictor report

Status: **accepted engineering checkpoint — all declared simulation and
implementation gates are complete**.

This report compares WB-N5G against the frozen `WB-N4-A-20PASS` baseline.
WB-N5G is a preliminary-round performance checkpoint, not a claim of board
stability or Linux readiness.

## 1. Result summary

| Metric | WB-N4-A | WB-N5G | Change |
|---|---:|---:|---:|
| Official functional tests | 58/58 | **58/58 PASS** | 0 failures |
| Official performance tests | 20/20 | **20/20 PASS** | 0 failures |
| CPU-cycle geometric mean | 180,049.55 | **175,018.46** | **-2.794% cycles / 1.028746x (+2.875%) speedup** |
| SoC-cycle geometric mean | 200,777.78 | **197,295.19** | **-1.734% cycles / 1.017652x (+1.765%) speedup** |
| Diagnostic CPI proxy* | 1.743747 | **1.730609** | **-0.753%** |
| Diagnostic IPC proxy* | 0.573478 | **0.577831** | **+0.759%** |
| Total branch mispredictions | 67,167 | **58,433** | **-8,734 (-13.003%)** |
| Dhrystone DMIPS/MHz | 0.665053 | **0.687714** | **+3.407%** |
| Routed WNS at 10.000 ns | +0.139 ns | **+0.010 ns** | -0.129 ns |
| Delay-derived Fmax | 101.410 MHz | **100.100 MHz** | **-1.291%** |
| Routed LUT | 43,377 | **43,857** | +480 (**+1.107%**) |
| Routed FF | 17,006 | **17,016** | +10 (**+0.059%**) |
| BRAM tiles | 28.5 | **30.5** | +2.0 (**+7.018%**) |
| DSP | 4 | **4** | 0 |

The routed timing result is from a complete `report_timing_summary`, not the
router's intermediate estimate.  TNS is zero and the route-status report has
zero routing errors.

`*` The CPI/IPC values are aggregate diagnostic proxies computed as
`sum(CPU Count) / sum(commit)` and its inverse.  The counters include
boot/measurement regions that do not exactly match the official scoring
window, so these are useful for release-to-release diagnosis but are not
architectural CPI/IPC.

## 2. Architecture and innovation

The base remains NOP-Core's out-of-order, multiple-issue LoongArch32
microarchitecture:

- four-instruction fetch;
- three-wide decode/rename and in-order retirement;
- 32-entry ROB;
- three integer, one multiply/divide and one load/store issue path;
- separate I-cache and D-cache;
- speculative global branch history, BTB and RAS.

WB-N5G adds a tournament direction predictor:

1. the inherited global-history PHT and a PC-only bimodal PHT predict in
   parallel;
2. a per-PC 2-bit chooser selects global or bimodal;
3. both predictors train on every resolved conditional branch;
4. the chooser trains only when the predictors disagree and exactly one is
   correct;
5. prediction-time counters and history travel through the pipeline so
   recovery and training use the correct snapshot.

The PPA innovation is a packed 4-bit, 4096-entry PC table:

```text
{ chooser[1:0], bimodal_counter[1:0] }
```

The first correct N5E implementation used two independent 8192-entry 2-bit
tables.  Packing and capacity tuning reduced routed LUTs from 46,157 to
43,857 (**-4.983%**) and BRAM from 32.5 to 30.5 (**-6.154%**), while the
completed compact-table spot tests remained cycle-identical.  Post-route
physical optimization moved one replicated ROB-PC register and recovered the
last 89 ps, changing WNS from -0.079 ns to +0.010 ns without changing logic
or resource counts.

## 3. Performance interpretation

The final compact-table replay is recorded at:

- `reports/WB-N5G/official/all20_summary.md`
- `reports/WB-N5G/official/n5g_vs_n4.md`

All 20 compact N5G tests passed.  Every recorded cycle and performance-counter
field is identical to the earlier 8192-entry N5E characterization; the
following results therefore belong to the final packed 4096-entry build:

- CPU-cycle geometric-mean speedup: 1.028746x (**+2.875%**);
- SoC-cycle geometric-mean speedup: 1.017652x (**+1.765%**);
- total branch mispredictions: 67,167 to 58,433 (**-13.003%**);
- best program, `stringsearch`: 41,944 to 31,808 cycles
  (**-24.166%**);
- only cycle regression, `bubble_sort`: 161,850 to 162,623
  (**+0.478% cycles**).

At the same 100 MHz constraint, the cycle-only speedup is the most direct
comparison.  Including each version's delay-derived Fmax gives:

| Combined estimate | WB-N4-A | WB-N5G | Change |
|---|---:|---:|---:|
| `Fmax / CPU-cycle geomean` | 1.000000 | **1.015462** | **+1.546%** |
| `Fmax / SoC-cycle geomean` | 1.000000 | **1.004511** | **+0.451%** |
| 50:50 frequency + CPU-cycle normalized score | 1.000000 | **1.007917** | **+0.792%** |
| 50:50 frequency + SoC-cycle normalized score | 1.000000 | **1.002370** | **+0.237%** |

These are transparent normalized estimates, not a claim about the organizer's
unpublished final ranking formula.  The small frequency loss offsets part of
the branch-prediction cycle gain but does not erase it.

## 4. Source difference and open-source provenance

Compared with `WB-N4-A-20PASS` under `src/`:

| Item | Difference |
|---|---:|
| Files changed | 6 |
| Insertions / deletions | +110 / -10 |
| Churn / 8,600 N4 source lines | **1.395%** |
| Net source growth | 100 lines (**1.163%**) |

Compared mechanically with the local frozen NOP-Core `src` tree:

| Item | Difference |
|---|---:|
| Files changed | 11 |
| Insertions / deletions | +452 / -57 |
| Churn / 8,336 upstream source lines | **6.106%** |
| Net source growth | 395 lines (**4.738%**) |

These percentages measure text churn, not originality or performance.  The
base is NOP-Core at commit
`1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`, MIT licensed.  Tournament
prediction is a standard published concept; the SpinalHDL implementation,
packed-table organization and integration were written in this repository.
No source lines from another tournament predictor were copied.

## 5. Validation evidence

- Functional 58/58:
  `reports/WB-N5G/official/func58_summary.md`
- First route:
  `reports/WB-N5G/implementation/timing_route_10ns.rpt`
- Final post-route timing:
  `reports/WB-N5G/implementation/timing_route_post_physopt_10ns.rpt`
- Final utilization:
  `reports/WB-N5G/implementation/utilization_route_post_physopt.rpt`
- Final route status:
  `reports/WB-N5G/implementation/route_status_post_physopt.rpt`
- Official performance 20/20:
  `reports/WB-N5G/official/all20_summary.md`
- Per-program N4 comparison:
  `reports/WB-N5G/official/n5g_vs_n4.md`
- Design description:
  `docs/WB-N5_DESIGN.md`

The generated simulation and synthesis RTL come from the same `N5` profile;
only performance-event observability is removed from the synthesis build.

## 6. Regressions, rejected experiments and limits

- Frequency is 1.291% below N4's delay-derived Fmax despite meeting 100 MHz.
- The predictor costs 480 LUT, 10 FF and two BRAM tiles versus N4.
- The completed large-table characterization has a 0.478% `bubble_sort`
  cycle regression; final compact-table numbers are reported without hiding
  regressions.
- Four D-cache prefetch/line-size experiments were rejected because they
  regressed cycles or failed correctness.  Their RTL is not in N5G.
- WB-N5G is the preliminary-round direct-translation profile.  It is not yet
  the Linux profile; Linux work must re-enable the full MMU/TLB path and rerun
  all gates.
- The formal 100 MHz route closes by only 0.010 ns.  Until a board sweep is
  complete, 99 MHz is the conservative operating recommendation because it
  provides more than 0.1 ns of mathematical timing margin on this route.
- No board-stability sweep or final bitstream test is claimed here.
- This release does **not** meet the earlier aspirational 1.5x target.  Its
  value is a verified branch-prediction gain and a clean architectural
  checkpoint, not a fabricated 50% claim.
