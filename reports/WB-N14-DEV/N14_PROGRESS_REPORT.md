# WB-N14 development progress

Status: **accepted; superseded by the formal WB-N14 release report**.

## Implemented scope

- `WeBattleICachePlugin`: 191 nonblank lines; independently controls array
  lookup, hit selection, refill burst, packet assembly and cache invalidation.
- `WeBattleDirectionPredictor` and `WeBattlePredictorBTBPlugin`: 280 nonblank
  lines; independently control global/tournament prediction, BTB lookup and
  update, speculative GHR movement and recovery.
- Two elaboration switches plus `N14NOICACHE` / `N14NOPRED` profiles preserve
  direct N13 A/B fallbacks.

Frozen accepted-weight candidate:

`(1,960 + 253 + 215) / 4,791 = 50.68%`

This candidate percentage is not accepted until every release gate passes.

## Evidence so far

| Gate | Result |
|---|---|
| Scala compile | PASS |
| Simulation RTL elaboration | PASS |
| Synthesis RTL elaboration | PASS |
| Official bitcount smoke | PASS, CPU 23,517 cycles |
| N13 bitcount comparison | exact cycle/counter match |
| Independent synthesis | PASS, 0 errors |
| Official 58 functional | **PASS, 58/58** |
| Official 20 performance | **PASS, 20/20** |
| Routed implementation | **PASS, WNS +0.006 ns** |

All 20 official CPU counts, SoC counts, cache-miss counters and branch
misprediction counters are exactly equal to WB-N13. CPU/SoC geometric means
remain 172,821.02 / 194,719.81 cycles.

## Synthesis comparison

| Metric | N13 | N14 | Change |
|---|---:|---:|---:|
| WNS at 10 ns | -1.298 ns | -0.852 ns | **+0.446 ns** |
| TNS | -62.487 ns | -29.277 ns | +33.210 ns |
| Failing endpoints | 88 | 59 | -29 |
| LUT | 45,116 | 45,700 | +584 (+1.29%) |
| FF | 20,486 | 20,505 | +19 (+0.09%) |
| BRAM | 30.5 | 30.5 | unchanged |
| DSP | 4 | 4 | unchanged |

The synthesis result is encouraging but does not predict final routed WNS
with certainty. Promotion still requires a non-negative post-route result.

## Secondary audit metrics

- independent `WeBattle*.scala` files: 21;
- independent nonblank lines: 2,484;
- changed or added upstream/current source files: 42;
- raw textual churn: 51.39%.

These are audit metrics, not proof of originality. The primary claim remains
component-weighted ownership after validation.

## Final routed result

- WNS/TNS/failing endpoints: +0.006 ns / 0 / 0;
- routing errors: 0;
- LUT/FF/BRAM/DSP: 45,309 / 20,518 / 30.5 / 4;
- route-derived Fmax: 100.060 MHz;
- combined estimate versus N13: 0.998599x (-0.140%).

## Packaged RTL

- simulation SHA-256:
  `58BA10635A9E575AF5C393705AC96A68ED873E658A37AA3BA589869813149F7F`
- synthesis SHA-256:
  `4180578FF78A01A1A65E2C22DB046FB42FDB744EDFFB1918929C92063E308A53`
