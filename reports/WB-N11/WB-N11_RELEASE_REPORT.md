# WB-N11 release report

## Verdict

WB-N11 passes official 58/58 functionality, official 20/20 performance and
100 MHz routed timing. It is accepted as the new ownership head at
**27.68%** component-weighted coverage.

N11 is not the strict performance head. Its official CPU-cycle geometric mean
is 0.0073% worse than N10 and its route-derived Fmax is 0.270% lower. The
combined estimate is therefore 0.278% below N10, while remaining 0.172% above
N9 and 0.125% above N7.

## Results

| Metric | N10 | N11 | Change |
|---|---:|---:|---:|
| CPU-cycle geometric mean | 172,808.44 | 172,821.02 | -0.0073% |
| SoC-cycle geometric mean | 194,705.69 | 194,719.81 | -0.0073% |
| Route-derived Fmax | 100.361 MHz | 100.090 MHz | -0.270% |
| Routed WNS | +0.036 ns | +0.009 ns | -0.027 ns |
| LUT | 45,385 | 45,562 | +177 |
| FF | 20,347 | 20,495 | +148 |
| BRAM / DSP | 30.5 / 4 | 30.5 / 4 | unchanged |

All 20 tests pass. N11 equals N10 exactly on 19 tests; `fireye_I2` changes
from 274,531 to 274,931 CPU cycles.

## Difference and provenance

The processor remains an attributed NOP-Core derivative under the MIT license.
N11 accepts the independently maintained MulDiv queue/controller boundary,
while retaining the Xilinx multiplier IP and Spinal divider arithmetic
primitives. Accepted coverage is:

`1,326 / 4,791 = 27.68%`

Secondary audit metrics are 13 independent WeBattle source files, 1,369
nonblank lines and 34.08% raw textual churn. These audit metrics are not used
as proof of originality.

## Packaged RTL

- simulation `wb_raw_top.v` SHA-256:
  `39999A96070EE707E551EE070773B538B452910CB0D8ED3CD0B17F7C7311DCB8`
- synthesis `wb_raw_top.v` SHA-256:
  `D57710F758B8A4DD1356D091A52E8E9A104F5A0945D290D7272CC9C503CB417D`
