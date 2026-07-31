# WB-N12 release report

## Verdict

WB-N12 passes official 58/58 functionality, official 20/20 performance and
100 MHz routed timing. It is accepted as the ownership head at **30.16%**
component-weighted coverage.

N12 replaces memory issue-queue state, append, head removal, compaction,
wakeup and flush logic with a specialized oldest-only FIFO. All 20 official
CPU and SoC cycle counts are exactly equal to N11. The routed design uses
1,594 fewer LUTs and 95 fewer flip-flops than N11 and improves WNS by 0.001 ns.

N10 remains the strict performance head: N12's combined route-derived score is
0.268% below N10.

## Results

| Metric | N11 | N12 | Change |
|---|---:|---:|---:|
| Official functionality | 58/58 | 58/58 | unchanged |
| Official performance | 20/20 | 20/20 | unchanged |
| CPU-cycle geometric mean | 172,821.02 | 172,821.02 | 0.000% |
| SoC-cycle geometric mean | 194,719.81 | 194,719.81 | 0.000% |
| Route-derived Fmax | 100.090 MHz | 100.100 MHz | +0.010% |
| Routed WNS | +0.009 ns | +0.010 ns | +0.001 ns |
| Routed TNS / failing endpoints | 0 / 0 | 0 / 0 | unchanged |
| LUT | 45,562 | 43,968 | **-1,594 (-3.50%)** |
| FF | 20,495 | 20,400 | **-95 (-0.46%)** |
| BRAM / DSP | 30.5 / 4 | 30.5 / 4 | unchanged |
| Combined estimate | 1.000000x | 1.000100x | **+0.010%** |

Against N10, N12 has 0.0073% more geometric-mean CPU cycles and about 0.260%
less route-derived Fmax, for a combined estimate of 0.997325x.

## Difference and provenance

The processor remains an attributed NOP-Core derivative under the MIT license.
N12 accepts the independently maintained memory issue-queue state boundary.
The surrounding LSU, caches, physical-register broadcasts and execution
datapaths remain inherited and are not counted.

Accepted coverage is:

`1,445 / 4,791 = 30.16%`

Secondary audit metrics are 14 independent WeBattle source files, 1,458
nonblank lines and 35.58% raw textual churn. These metrics are disclosed for
auditability, not presented as proof of originality.

## Packaged RTL

- simulation `wb_raw_top.v` SHA-256:
  `E466E1BF341B16CB15FBE54D6B375A0FFDAA818E21875E8335B109A8771A415A`
- synthesis `wb_raw_top.v` SHA-256:
  `4D8017742F565735FD795E19A606061BA234D95B047750929F69699DFCEF4AA7`

## Evidence

- functional: `official/func58_summary.md`;
- performance: `official/all20_summary.md`, `official/all20_summary.csv`;
- N11 comparison: `official/n12_vs_n11.md`,
  `official/n12_vs_n11.csv`;
- physical: `implementation/timing_route_post_physopt_10ns.rpt`,
  `implementation/utilization_route_post_physopt.rpt`,
  `implementation/route_status_post_physopt.rpt`.
