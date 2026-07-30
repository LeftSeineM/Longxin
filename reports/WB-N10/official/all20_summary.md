# WB-N10 official 20-test performance result

- Result: **20 / 20 PASS**
- CPU-count geometric mean: **172,808.44**
- SoC-count geometric mean: **194,705.69**
- CPU speedup versus N9: **1.001096x (+0.110%)**
- SoC speedup versus N9: **1.002161x (+0.216%)**
- Faster / tie / slower versus N9: **13 / 1 / 6**

| Test | N10 SoC | N10 CPU | vs N9 CPU |
|---|---:|---:|---:|
| bitcount | 26393 | 23517 | 1.403% |
| bubble_sort | 174267 | 158307 | 0.111% |
| coremark | 409980 | 372225 | 0.147% |
| crc32 | 196404 | 178432 | 0.004% |
| dhrystone | 9526 | 8175 | -2.251% |
| quick_sort | 237837 | 215362 | 0.423% |
| select_sort | 81335 | 73824 | 0.279% |
| sha | 191273 | 173585 | 0.815% |
| stream_copy | 37748 | 33648 | 0.565% |
| stringsearch | 48362 | 31317 | 0.016% |
| fireye_A0 | 3424226 | 3111710 | 0.000% |
| fireye_B2 | 64960 | 58573 | 0.930% |
| fireye_C0 | 259917 | 235268 | -0.008% |
| fireye_D1 | 593353 | 538189 | -0.0002% |
| fireye_I2 | 303138 | 274531 | -0.006% |
| inner_product | 1364525 | 1239807 | 0.0005% |
| lookup_table | 250682 | 226495 | -0.172% |
| loop_induction | 853414 | 774959 | 0.0005% |
| my_memcmp | 285839 | 259183 | 0.002% |
| minmax_sequence | 296520 | 268503 | -0.022% |

The suite was run as five balanced official selections. The aggregated CSV
contains exactly 20 unique PASS rows.
