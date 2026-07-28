# WeBattle-Core Changelog

## WB-N1（工程基线，2026-07-29）

- 新增 `WeBattleRawTop`，从生成源头移除上游超大 Difftest、RAT、PRF、
  Cache 调试端口，只保留2026集成所需信号。
- 新增 `WeBattleProfiles.N0/N1`，使基线和实验配置可重复生成。
- 增加 LA32R `CPUCFG` 解码和安全的未实现配置返回值，兼容2026启动代码。
- PHT 从纯 GHR/PC 拼接索引扩展为可配置的 bank-safe gshare：
  在不扰动四路物理bank低位的前提下，把全局历史异或进PC索引。
- 首轮bitcount显示gshare实验把误预测从1,013增加到1,667，N1默认配置已
  回退到gselect；实验实现保留用于后续参数扫描，不进入候选配置。
- 弱跳转PHT冷启动实验使bitcount误预测由1,013增加到1,060、CPU周期由
  25,114增加到26,217，N1默认配置同样回退；参数化实现保留用于完整负载扫描。
- 16项RAS实验已否决：CoreMark周期没有收益，而`recoverTop`扩位导致综合LUT从43.7k级增至50.6k；N1默认回退为8项。
- 增加提交、双/三提交、访存指令、分支、预测分支、预测错误、
  I-Cache miss、D-Cache miss计数。
- 增加独立的来源、架构、验证和版本文档。
- 最终配置通过官方 bitcount（25,114 CPU周期）和CoreMark
  （404,385 CPU周期，2.472891 CoreMark/MHz）。
- xc7a200t-2最终布局布线在10.2 ns约束下WNS为+0.115 ns，即
  98.04 MHz时序通过；10.0 ns下WNS为-0.085 ns，因此不宣称100 MHz通过。
- 相同约0.115 ns时序裕量下，上游包装为95.24 MHz，WB-N1为98.04 MHz，
  稳定频率口径提升约2.94%；两个已复测程序的CPU周期无变化。

## WB-N0（冻结基线）

- 上游 NOP-Core 提交
  `1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`。
- 增加2026 `core_top` 包装、真实退休调试口和 `CPUCFG` 兼容。
- 阶段证据：旧包装通过58点功能测试；`CPUCFG`版本通过 bitcount、
  CoreMark、loop induction、stream copy。
