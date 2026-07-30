# WB-N10 release report

## Verdict

WB-N10 is the accepted ownership and performance head. It replaces the
integer issue-queue state engine, passes all official and physical gates, and
raises accepted component-weighted coverage from 20.62% to **22.63%**.

## Performance

| Metric | N7 | N9 | N10 |
|---|---:|---:|---:|
| CPU-cycle geometric mean | 174,999.32 | 172,997.81 | **172,808.44** |
| SoC-cycle geometric mean | 197,280.70 | 195,126.53 | **194,705.69** |
| Route-derived Fmax | 101.225 MHz | 100.020 MHz | **100.361 MHz** |
| Combined score vs N7 | 1.000000x | 0.999530x | **1.004039x** |
| Combined score vs N9 | 1.000470x | 1.000000x | **1.004512x** |

Against N9, 13 tests are faster, one is cycle-identical and six are slower.
The CPU-cycle geometric mean improves by 0.110%; routed Fmax improves by
0.341%; their combined estimate improves by **0.451%**.

## Validation and PPA

| Gate | Result |
|---|---:|
| Official functional | **58/58 PASS** |
| Official performance | **20/20 PASS** |
| Routed WNS / TNS | **+0.036 ns / 0** |
| Routing errors | **0** |
| LUT | 45,385 |
| FF | 20,347 |
| BRAM | 30.5 |
| DSP | 4 |

Relative to N9, LUT use increases by 1,341 (+3.045%), FF decreases by five
(-0.025%), and BRAM/DSP use is unchanged.

## Ownership and provenance

The surrounding processor remains an attributed derivative of NOP-Core,
upstream commit `1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`, under the MIT
license.

N10 adds a separately maintained dense issue-state implementation with
bounded three-entry compaction and final-slot wakeup folding. The accepted
integer queue boundary adds 96 weighted lines:

`1,084 / 4,791 = 22.63%`

Secondary audit metrics are 13 independent `WeBattle*.scala` files, 1,367
nonblank lines, and 33.99% raw upstream/current textual churn. Textual churn is
reported for audit and is not presented as originality by itself.

The full N10A queue/MDU experiment is disclosed but rejected for negative WNS.
Its unaccepted modules are not included in the 22.63% numerator.

## Packaged RTL hashes

- simulation `core_top.sv`:
  `2493959964E8C57E37A5B78545793174483EB64298CDB9EA1FA8193C999588DE`
- simulation `wb_raw_top.v`:
  `FA234FBFAE6383CFACD913031A8FFEE5EBDF2FD78BF4F4E9D6CDB641F9589468`
- synthesis `core_top.sv`:
  `2493959964E8C57E37A5B78545793174483EB64298CDB9EA1FA8193C999588DE`
- synthesis `wb_raw_top.v`:
  `D72451BC2556C654572CC43161E19FAC969A81E7EC4AC8C568D46485A83AD553`
