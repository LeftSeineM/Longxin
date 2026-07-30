# WB-N9 official 20-test performance result

- Result: 20 / 20 PASS
- SoC-count geometric mean: 195126.53
- CPU-count geometric mean: 172997.81
- Logs: D:\NSCSCC2026\PulseLA\build\webattle_n9_all20\official_all_20.log; D:\NSCSCC2026\PulseLA\build\webattle_n9_remaining11\official_stringsearch_fireye_A0_fireye_B2_fireye_C0_fireye_D1_fireye_I2_inner_product_lookup_table_loop_induction_my_memcmp_minmax_sequence.log
- Note: performance counters include boot/measurement regions that differ from official CPU Count; Commit/CPU is diagnostic, not architectural IPC.

| Test | SoC cycles | CPU cycles | Commit | Commit/CPU* | I$ miss | D$ miss | Branch MP | MP rate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| bitcount | 26756 | 23847 | 48530 | 2.0351 | 252 | 55 | 837 | 7.89% |
| bubble_sort | 174459 | 158482 | 115071 | 0.7261 | 210 | 84 | 4756 | 15.17% |
| coremark | 410581 | 372771 | 394738 | 1.0589 | 2254 | 128 | 9457 | 10.88% |
| crc32 | 196414 | 178440 | 193715 | 1.0856 | 317 | 74 | 929 | 1.04% |
| dhrystone | 9524 | 7991 | 165902 | 20.7611 | 1235 | 116 | 5142 | 9.67% |
| quick_sort | 238841 | 216274 | 161792 | 0.7481 | 247 | 266 | 9029 | 19.89% |
| select_sort | 81562 | 74030 | 96520 | 1.3038 | 214 | 83 | 1713 | 5.50% |
| sha | 192828 | 174999 | 204308 | 1.1675 | 701 | 90 | 1335 | 3.24% |
| stream_copy | 37957 | 33838 | 32106 | 0.9488 | 208 | 186 | 504 | 6.33% |
| stringsearch | 48365 | 31322 | 305369 | 9.7493 | 2567 | 121 | 11535 | 12.44% |
| fireye_A0 | 3424226 | 3111710 | 549318 | 0.1765 | 237 | 15328 | 621 | 1.34% |
| fireye_B2 | 65558 | 59118 | 55799 | 0.9439 | 273 | 159 | 1176 | 8.18% |
| fireye_C0 | 259899 | 235250 | 157939 | 0.6714 | 282 | 264 | 3349 | 12.55% |
| fireye_D1 | 593353 | 538188 | 423852 | 0.7876 | 268 | 1887 | 2618 | 2.10% |
| fireye_I2 | 303118 | 274515 | 303276 | 1.1048 | 335 | 104 | 2537 | 2.51% |
| inner_product | 1364530 | 1239813 | 469465 | 0.3787 | 248 | 4401 | 548 | 0.68% |
| lookup_table | 250253 | 226105 | 150963 | 0.6677 | 418 | 485 | 663 | 3.92% |
| loop_induction | 853419 | 774963 | 549576 | 0.7092 | 229 | 2071 | 512 | 0.40% |
| my_memcmp | 285843 | 259187 | 153730 | 0.5931 | 223 | 868 | 453 | 0.72% |
| minmax_sequence | 296252 | 268443 | 277211 | 1.0327 | 511 | 95 | 1545 | 2.21% |
