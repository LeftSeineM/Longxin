# WB-N14 release report

## Verdict

WB-N14 passes official 58/58 functionality, official 20/20 performance and
100 MHz routed timing. It is accepted as the ownership head at **50.68%**
component-weighted coverage, the first validated checkpoint above the 50%
target.

N14 replaces the instruction-cache controller and the complete BTB/direction
predictor/history-control cluster. All 20 official CPU/SoC cycles, cache-miss
counters and branch-misprediction counters are exactly equal to N13. Final
post-route WNS is +0.006 ns with no timing or routing failure.

N10 remains the strict performance head. N14's combined route-derived score is
0.307% below N10 and 0.140% below N13 because its final route margin is smaller.

## Results

| Metric | N13 | N14 | Change |
|---|---:|---:|---:|
| Official functionality | 58/58 | 58/58 | unchanged |
| Official performance | 20/20 | 20/20 | unchanged |
| CPU-cycle geometric mean | 172,821.02 | 172,821.02 | 0.000% |
| SoC-cycle geometric mean | 194,719.81 | 194,719.81 | 0.000% |
| Route-derived Fmax | 100.200 MHz | 100.060 MHz | -0.140% |
| Routed WNS | +0.020 ns | +0.006 ns | -0.014 ns |
| Routed TNS / failing endpoints | 0 / 0 | 0 / 0 | unchanged |
| LUT | 44,729 | 45,309 | +580 (+1.30%) |
| FF | 20,501 | 20,518 | +17 (+0.08%) |
| BRAM / DSP | 30.5 / 4 | 30.5 / 4 | unchanged |
| Combined estimate | 1.000000x | 0.998599x | -0.140% |

## Difference and provenance

The processor remains an attributed NOP-Core derivative under the MIT license.
N14 accepts independent implementations for:

1. I-cache array request, tag comparison, hit selection and packet assembly;
2. aligned AXI refill sequencing, word installation, tag/LRU/valid update and
   architectural invalidate operations;
3. global and packed bimodal/chooser prediction tables;
4. BTB lookup priority, target metadata and allocation/update/invalidation;
5. speculative global-history movement and committed misprediction recovery.

RAM primitives, AXI types and the surrounding pipeline framework remain reused
and attributed. Accepted coverage is:

`2,428 / 4,791 = 50.68%`

Secondary audit metrics are 21 independent WeBattle source files, 2,484
nonblank lines, 42 changed or added source files and 51.39% raw textual churn.
These are disclosed for auditability and are not substituted for the validated
component-ownership metric.

## Packaged RTL

- simulation `wb_raw_top.v` SHA-256:
  `58BA10635A9E575AF5C393705AC96A68ED873E658A37AA3BA589869813149F7F`
- synthesis `wb_raw_top.v` SHA-256:
  `4180578FF78A01A1A65E2C22DB046FB42FDB744EDFFB1918929C92063E308A53`

## Evidence

- functional: `official/func58_summary.md`;
- performance: `official/all20_summary.md`, `official/all20_summary.csv`;
- N13 comparison: `official/n14_vs_n13.md`, `official/n14_vs_n13.csv`;
- physical: `implementation/timing_route_post_physopt_10ns.rpt`,
  `implementation/utilization_route_post_physopt.rpt`,
  `implementation/route_status_post_physopt.rpt`;
- ownership audit: `ownership_measurement.md`.
