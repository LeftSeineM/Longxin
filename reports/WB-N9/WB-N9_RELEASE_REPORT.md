# WB-N9 release report

## Verdict

WB-N9 is the accepted **ownership head**, not the strict performance head.
It passes every promotion gate and raises component-weighted independent
coverage to **20.62%**. Its official CPU-cycle geometric mean improves by
**1.157%** versus N7, but its route-derived Fmax falls by **1.190%**; the
combined estimate is therefore essentially tied at **0.999530x (-0.047%)**.

## Open-source difference

Upstream base: NOP-Core commit
`1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495` under the MIT license.

N9's accepted WeBattle-owned boundaries are:

- fetch queue and checkpointed return-address stack;
- age-ordered integer issue selection and wakeup;
- integer ALU, comparison and branch-resolution datapath;
- three-wide speculative/committed rename and free-register state;
- physical-register value/readiness state and bypass;
- explicit-occupancy ROB storage and completion;
- ordered retirement, precise recovery and architectural side effects.

Component-weighted coverage is **988 / 4,791 = 20.62%**. Secondary audit
metrics are eleven independent `WeBattle*.scala` files, 1,101 nonblank code
lines, and **26.04%** raw upstream/current textual churn. Textual churn is not
used as an originality claim.

## Validation

| Gate | WB-N9 result |
|---|---:|
| Official functional | **58/58 PASS** |
| Official performance | **20/20 PASS** |
| Routed WNS / TNS | **+0.002 ns / 0** |
| Failed or unrouted nets | **0** |
| LUT | 44,044 |
| FF | 20,352 |
| BRAM | 30.5 |
| DSP | 4 |

## Performance versus N7

| Metric | N7 | N9 | Change |
|---|---:|---:|---:|
| CPU-cycle geometric mean | 174,999.32 | 172,997.81 | **1.011570x / +1.157%** |
| SoC-cycle geometric mean | 197,280.70 | 195,126.53 | **1.011040x / +1.104%** |
| Routed WNS | +0.121 ns | +0.002 ns | -0.119 ns |
| Route-derived Fmax | 101.225 MHz | 100.020 MHz | **-1.190%** |
| Fmax/CPU-cycle estimate | 1.000000x | 0.999530x | **-0.047%** |
| LUT | 42,855 | 44,044 | **+2.774%** |
| FF | 17,036 | 20,352 | **+19.465%** |

N9 is faster in 19 of 20 CPU-cycle results. `stream_copy` is the only
regression at **-0.466%**. The largest gains are `quick_sort` **+4.040%**,
`dhrystone` **+3.444%**, `bubble_sort` **+2.546%**, and `bitcount`
**+2.250%**.

## Packaged RTL

- `generated/WB-N9/core_top.sv` SHA-256:
  `2493959964E8C57E37A5B78545793174483EB64298CDB9EA1FA8193C999588DE`
- `generated/WB-N9/wb_raw_top.v` SHA-256:
  `59C463E3BD6432A9B7E6EA107DD136E9BF2F987AB94519E3C03167C62C489F29`
- `generated/WB-N9-synth/core_top.sv` SHA-256:
  `2493959964E8C57E37A5B78545793174483EB64298CDB9EA1FA8193C999588DE`
- `generated/WB-N9-synth/wb_raw_top.v` SHA-256:
  `5560E7DFAE409B01FFB8830374FC0555E7E893D5311561AB3897552F29A3EFBD`

Official simulation used `generated/WB-N9`; routed implementation used the
synthesis-profile sibling `generated/WB-N9-synth`. Both were generated from
the same N9 source/configuration state.

N10 must preserve N9's functionality and coverage while recovering timing
margin. It is promoted only if 58/58, 20/20, WNS >= 0 and the combined
Fmax/cycle score all pass.
