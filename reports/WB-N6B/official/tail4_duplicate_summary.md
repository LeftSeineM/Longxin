# WB-N6B duplicate tail-4 consistency run

- Result: 4 / 20 PASS
- SoC-count geometric mean: 284918.32
- CPU-count geometric mean: 257866.69
- Logs: D:\NSCSCC2026\PulseLA\build\wb_n6b_perf_tail4\official_fireye_I2_lookup_table_my_memcmp_minmax_sequence.log
- Note: performance counters include boot/measurement regions that differ from official CPU Count; Commit/CPU is diagnostic, not architectural IPC.

| Test | SoC cycles | CPU cycles | Commit | Commit/CPU* | I$ miss | D$ miss | Branch MP | MP rate |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| fireye_I2 | 305286 | 276480 | 303132 | 1.0964 | 336 | 105 | 2536 | 2.51% |
| lookup_table | 252968 | 228572 | 150780 | 0.6597 | 417 | 487 | 632 | 3.75% |
| my_memcmp | 286811 | 259708 | 153514 | 0.5911 | 222 | 872 | 446 | 0.71% |
| minmax_sequence | 297518 | 269407 | 276974 | 1.0281 | 512 | 96 | 1474 | 2.12% |
