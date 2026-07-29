# WB-N4-A official 20-test performance baseline

- Result: 20 / 20 PASS
- SoC-count geometric mean: 200777.78
- CPU-count geometric mean: 180049.55
- Logs: D:\NSCSCC2026\PulseLA\build\wb_n4_bitcount\official_all_20.log; D:\NSCSCC2026\PulseLA\build\wb_n4_remaining6\official_fireye_I2_inner_product_lookup_table_loop_induction_my_memcmp_minmax_sequence.log
- Note: performance counters include boot/measurement regions that differ from official CPU Count; Commit/CPU is diagnostic, not architectural IPC.

| Test | SoC cycles | CPU cycles | Commit | Commit/CPU* | I$ miss | D$ miss | Branch MP | MP rate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| bitcount | 28174 | 25114 | 48368 | 1.9259 | 282 | 56 | 1013 | 9.60% |
| bubble_sort | 178210 | 161850 | 114900 | 0.7099 | 224 | 84 | 4760 | 15.21% |
| coremark | 446188 | 404385 | 392530 | 0.9707 | 2604 | 145 | 11457 | 13.29% |
| crc32 | 197008 | 178957 | 193490 | 1.0812 | 362 | 74 | 1062 | 1.19% |
| dhrystone | 9779 | 8558 | 163797 | 19.1396 | 1375 | 224 | 6545 | 12.47% |
| quick_sort | 251438 | 227879 | 161612 | 0.7092 | 257 | 273 | 9227 | 20.36% |
| select_sort | 86804 | 78753 | 96388 | 1.2239 | 221 | 83 | 2072 | 6.66% |
| sha | 196187 | 177654 | 203930 | 1.1479 | 794 | 96 | 1743 | 4.25% |
| stream_copy | 38248 | 33876 | 31947 | 0.9431 | 219 | 187 | 598 | 7.56% |
| stringsearch | 53207 | 41944 | 302135 | 7.2033 | 2832 | 122 | 12298 | 13.42% |
| fireye_A0 | 3425865 | 3113544 | 549126 | 0.1764 | 246 | 15334 | 767 | 1.66% |
| fireye_B2 | 68020 | 61340 | 55586 | 0.9062 | 295 | 159 | 1397 | 9.76% |
| fireye_C0 | 266289 | 241198 | 157795 | 0.6542 | 295 | 272 | 3511 | 13.18% |
| fireye_D1 | 599177 | 543646 | 423687 | 0.7793 | 284 | 1897 | 2880 | 2.31% |
| fireye_I2 | 307922 | 278867 | 303057 | 1.0867 | 378 | 108 | 2750 | 2.72% |
| inner_product | 1370802 | 1245307 | 469306 | 0.3769 | 266 | 4407 | 699 | 0.87% |
| lookup_table | 253440 | 228986 | 150744 | 0.6583 | 432 | 489 | 761 | 4.52% |
| loop_induction | 853972 | 775449 | 549426 | 0.7085 | 238 | 2075 | 694 | 0.54% |
| my_memcmp | 288083 | 261021 | 153562 | 0.5883 | 226 | 879 | 603 | 0.96% |
| minmax_sequence | 307929 | 278861 | 277010 | 0.9934 | 524 | 97 | 2330 | 3.34% |
