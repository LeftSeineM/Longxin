# WB-N13 release report

## Verdict

WB-N13 passes official 58/58 functionality, official 20/20 performance and
100 MHz routed timing. It is accepted as the ownership head at **40.91%**
component-weighted coverage.

N13 replaces five LSU and memory-control boundaries with independently
maintained WeBattle implementations. All 20 official CPU and SoC cycle counts
are exactly equal to N12. Routed WNS improves by 0.010 ns, while the larger
owned control scope costs 761 LUTs and 101 flip-flops.

N10 remains the strict performance head: N13's combined route-derived score is
0.168% below N10.

## Results

| Metric | N12 | N13 | Change |
|---|---:|---:|---:|
| Official functionality | 58/58 | 58/58 | unchanged |
| Official performance | 20/20 | 20/20 | unchanged |
| CPU-cycle geometric mean | 172,821.02 | 172,821.02 | 0.000% |
| SoC-cycle geometric mean | 194,719.81 | 194,719.81 | 0.000% |
| Route-derived Fmax | 100.100 MHz | 100.200 MHz | **+0.100%** |
| Routed WNS | +0.010 ns | +0.020 ns | **+0.010 ns** |
| Routed TNS / failing endpoints | 0 / 0 | 0 / 0 | unchanged |
| LUT | 43,968 | 44,729 | +761 (+1.73%) |
| FF | 20,400 | 20,501 | +101 (+0.50%) |
| BRAM / DSP | 30.5 / 4 | 30.5 / 4 | unchanged |
| Combined estimate | 1.000000x | **1.001002x** | **+0.100%** |

Against N10, N13 has 0.0073% more geometric-mean CPU cycles and 0.1603% less
route-derived Fmax, for a combined estimate of 0.998324x.

## Difference and provenance

The processor remains an attributed NOP-Core derivative under the MIT license.
N13 accepts independent implementations for:

1. effective-address, byte-enable and aligned store-data generation;
2. memory issue, register read, speculative wakeup and ROB/writeback control;
3. store-buffer state, retirement, append, flush, pop and forwarding;
4. byte/half/word load extraction and sign/zero extension;
5. uncached AXI store request and response control.

The D-cache arrays, lookup, refill and write machinery remain inherited.
Accepted coverage is:

`1,960 / 4,791 = 40.91%`

Secondary audit metrics are 19 independent WeBattle source files, 2,010
nonblank lines, 40 changed or added source files and 44.23% raw textual churn.
These metrics are disclosed for auditability, not presented as proof of
originality.

## Ablation conclusions

The complete N13 synthesis used 45,116 LUTs at WNS -1.298 ns before placement
and routing. Reverting only the store buffer improved synthesis WNS to
-0.738 ns but increased LUT use to 48,664; therefore that fallback was not
selected. Reverting address generation or load postprocessing also increased
LUT use. Execute and uncached-control ablations matched the full-cluster
synthesis summary. The accepted full cluster subsequently routed at +0.020 ns.

## Packaged RTL

- simulation `wb_raw_top.v` SHA-256:
  `F68D9944FF6308A8D4A2EE48E6CAB541E06EAC84E4FFED6A56C2DAAF1BEA60FE`
- synthesis `wb_raw_top.v` SHA-256:
  `E4E7CF9DF19CC9C8950B812A5D94D2D809912FA7B1BEF29F13AA0D4CCD2A33`

## Evidence

- functional: `official/func58_summary.md`;
- performance: `official/all20_summary.md`, `official/all20_summary.csv`;
- N12 comparison: `official/n13_vs_n12.md`, `official/n13_vs_n12.csv`;
- physical: `implementation/timing_route_post_physopt_10ns.rpt`,
  `implementation/utilization_route_post_physopt.rpt`,
  `implementation/route_status_post_physopt.rpt`;
- ownership audit: `ownership_measurement.md`.
