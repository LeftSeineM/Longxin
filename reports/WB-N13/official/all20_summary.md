# WB-N13 official 20-test performance result

- Result: 20 / 20 PASS
- SoC-count geometric mean: 194719.81
- CPU-count geometric mean: 172821.02
- Logs: D:\NSCSCC2026\PulseLA\build\webattle_n13_smoke_bitcount\official_bitcount.log; D:\NSCSCC2026\PulseLA\build\webattle_n13_smoke_bubble\official_bubble_sort.log; D:\NSCSCC2026\PulseLA\build\webattle_n13_coremark_full\official_coremark.log; D:\NSCSCC2026\PulseLA\build\webattle_n13_g1\official_fireye_A0_stringsearch_inner_product.log; D:\NSCSCC2026\PulseLA\build\webattle_n13_g2\official_crc32_dhrystone_fireye_D1_loop_induction.log; D:\NSCSCC2026\PulseLA\build\webattle_n13_g3\official_sha_stream_copy_fireye_B2_fireye_I2_lookup_table.log; D:\NSCSCC2026\PulseLA\build\webattle_n13_g4\official_quick_sort_select_sort_fireye_C0_my_memcmp_minmax_sequence.log
- Note: performance counters include boot/measurement regions that differ from official CPU Count; Commit/CPU is diagnostic, not architectural IPC.

| Test | SoC cycles | CPU cycles | Commit | Commit/CPU* | I$ miss | D$ miss | Branch MP | MP rate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| bitcount | 26393 | 23517 | 48530 | 2.0636 | 252 | 55 | 836 | 7.88% |
| bubble_sort | 174267 | 158307 | 115071 | 0.7269 | 210 | 84 | 4755 | 15.17% |
| coremark | 409980 | 372225 | 394447 | 1.0597 | 2249 | 127 | 9458 | 10.89% |
| crc32 | 196404 | 178432 | 193715 | 1.0857 | 317 | 74 | 928 | 1.04% |
| dhrystone | 9526 | 8175 | 165821 | 20.2839 | 1240 | 116 | 5156 | 9.70% |
| quick_sort | 237837 | 215362 | 161792 | 0.7513 | 248 | 269 | 9028 | 19.89% |
| select_sort | 81335 | 73824 | 96517 | 1.3074 | 214 | 83 | 1712 | 5.49% |
| sha | 191273 | 173585 | 204305 | 1.1770 | 701 | 90 | 1334 | 3.24% |
| stream_copy | 37748 | 33648 | 32109 | 0.9543 | 207 | 186 | 503 | 6.31% |
| stringsearch | 48362 | 31317 | 305387 | 9.7515 | 2566 | 121 | 11534 | 12.44% |
| fireye_A0 | 3424226 | 3111710 | 549318 | 0.1765 | 237 | 15328 | 620 | 1.34% |
| fireye_B2 | 64960 | 58573 | 55745 | 0.9517 | 273 | 159 | 1173 | 8.16% |
| fireye_C0 | 259917 | 235268 | 157939 | 0.6713 | 281 | 264 | 3348 | 12.54% |
| fireye_D1 | 593353 | 538189 | 423852 | 0.7876 | 268 | 1887 | 2617 | 2.10% |
| fireye_I2 | 303578 | 274931 | 303276 | 1.1031 | 336 | 105 | 2536 | 2.51% |
| inner_product | 1364525 | 1239807 | 469465 | 0.3787 | 248 | 4401 | 547 | 0.68% |
| lookup_table | 250682 | 226495 | 150963 | 0.6665 | 418 | 487 | 662 | 3.91% |
| loop_induction | 853414 | 774959 | 549576 | 0.7092 | 229 | 2071 | 511 | 0.40% |
| my_memcmp | 285839 | 259183 | 153730 | 0.5931 | 223 | 868 | 452 | 0.72% |
| minmax_sequence | 296520 | 268503 | 277211 | 1.0324 | 512 | 95 | 1544 | 2.21% |
