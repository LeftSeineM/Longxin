# WB-N7 official 20-test performance result

- Result: 20 / 20 PASS
- SoC-count geometric mean: 197280.7
- CPU-count geometric mean: 174999.32
- Logs: D:\NSCSCC2026\PulseLA\build\wb_n7_smoke\official_all_20.log; D:\NSCSCC2026\PulseLA\build\wb_n7_smoke\official_stringsearch_fireye_A0_fireye_B2_fireye_C0_fireye_D1_fireye_I2_inner_product_lookup_table_loop_induction_my_memcmp_minmax_sequence.log
- Note: performance counters include boot/measurement regions that differ from official CPU Count; Commit/CPU is diagnostic, not architectural IPC.

| Test | SoC cycles | CPU cycles | Commit | Commit/CPU* | I$ miss | D$ miss | Branch MP | MP rate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| bitcount | 27572 | 24396 | 48374 | 1.9829 | 254 | 55 | 833 | 7.89% |
| bubble_sort | 179019 | 162623 | 114897 | 0.7065 | 210 | 84 | 4758 | 15.21% |
| coremark | 418136 | 379449 | 393593 | 1.0373 | 2286 | 129 | 9721 | 11.24% |
| crc32 | 196669 | 178663 | 193541 | 1.0833 | 322 | 74 | 926 | 1.04% |
| dhrystone | 9640 | 8276 | 162139 | 19.5915 | 1224 | 114 | 5117 | 9.85% |
| quick_sort | 248863 | 225379 | 161642 | 0.7172 | 248 | 267 | 9021 | 19.90% |
| select_sort | 82891 | 75233 | 96367 | 1.2809 | 215 | 83 | 1704 | 5.48% |
| sha | 193191 | 175139 | 203876 | 1.1641 | 701 | 90 | 1339 | 3.26% |
| stream_copy | 37990 | 33681 | 31950 | 0.9486 | 208 | 186 | 494 | 6.24% |
| stringsearch | 48870 | 31741 | 300482 | 9.4667 | 2570 | 122 | 10535 | 11.56% |
| fireye_A0 | 3424653 | 3112284 | 549147 | 0.1764 | 237 | 15331 | 611 | 1.32% |
| fireye_B2 | 66275 | 59766 | 55643 | 0.9310 | 275 | 159 | 1172 | 8.18% |
| fireye_C0 | 264545 | 239653 | 157786 | 0.6584 | 283 | 268 | 3341 | 12.54% |
| fireye_D1 | 595582 | 540393 | 423654 | 0.7840 | 267 | 1885 | 2615 | 2.10% |
| fireye_I2 | 305286 | 276480 | 303132 | 1.0964 | 336 | 105 | 2535 | 2.51% |
| inner_product | 1369485 | 1243950 | 469252 | 0.3772 | 249 | 4402 | 540 | 0.68% |
| lookup_table | 252968 | 228572 | 150780 | 0.6597 | 417 | 487 | 632 | 3.75% |
| loop_induction | 853457 | 774993 | 549306 | 0.7088 | 232 | 2071 | 511 | 0.40% |
| my_memcmp | 286811 | 259708 | 153514 | 0.5911 | 222 | 872 | 446 | 0.71% |
| minmax_sequence | 297518 | 269407 | 276974 | 1.0281 | 512 | 96 | 1474 | 2.12% |
