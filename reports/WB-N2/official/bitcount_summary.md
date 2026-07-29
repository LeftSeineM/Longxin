# PulseLA 2026 官方性能测试汇总

- 配置：RUN_PERF_TEST，RUN_PERF_NO_DELAY 关闭，SIMU_USE_PLL=0，SIMU_USE_DDR=0
- 通过：1 / 1（本次选择）；官方全集覆盖：1 / 20
- 已完成项目总时间：0.28174 ms；几何平均：0.28174 ms
- 日志：D:\NSCSCC2026\PulseLA\build\wb_n2_direct_dmw_bitcount\official_bitcount.log

| # | 测试 | 状态 | SoC Count | CPU Count | 时间 (ms) | Commit | IPC | 双提交周期 | Memory | Branch | MP | I$ Miss | D$ Miss |
|---:|---|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| 1 | bitcount | PASS | 0x6e0e | 0x621a | 0.28174 | - | - | - | - | - | - | - | - |
| 2 | bubble_sort | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 3 | coremark | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 4 | crc32 | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 5 | dhrystone | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 6 | quick_sort | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 7 | select_sort | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 8 | sha | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 9 | stream_copy | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 10 | stringsearch | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 11 | fireye_A0 | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 12 | fireye_B2 | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 13 | fireye_C0 | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 14 | fireye_D1 | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 15 | fireye_I2 | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 16 | inner_product | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 17 | lookup_table | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 18 | loop_induction | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 19 | my_memcmp | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
| 20 | minmax_sequence | NOT_RUN | - | - | - | - | - | - | - | - | - | - | - |
