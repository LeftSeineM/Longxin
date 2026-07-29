# WeBattle-Core Changelog

## WB-N6B（独立取指队列替换，2026-07-30）

- 新增独立实现的 8 项、4 入 3 出 `WeBattleFetchQueue`，使用显式 occupancy、
  一热环形槽位选择、原子 flush 和仿真契约断言；N5 保留为 A/B 回退。
- 官方功能 58/58、性能 20/20 全通过；全部周期和事件计数与 WB-N5G
  逐字段一致，性能周期变化 **0.000%**。
- post-route phys-opt WNS +0.019 ns、TNS 0、无布线错误；延迟估算 Fmax
  100.190 MHz，相对 N5G **+0.090%**。
- route LUT 47,030，相对 N5G +3,173（**+7.235%**）；FF -0.041%，
  BRAM/DSP 不变。LUT 代价在报告中明确披露。
- N6A 动态索引版虽通过 58/58、20/20，但 LUT +7.641%、WNS -0.170 ns，
  被否决；N6C 无方向内联版 bitcount 卡住，被正确性门禁否决。
- 新增模块继承/自研归属矩阵。详见 `docs/WB_COMPONENT_OWNERSHIP.md`、
  `docs/WB-N6_DESIGN.md` 和 `docs/WB-N6_REPORT.md`。

## WB-N5G（紧凑型竞赛分支预测器，2026-07-29）

- 在 WB-N4-A 上加入 global-history PHT、PC-only bimodal PHT 与 2-bit
  chooser 组成的 tournament predictor。
- 将 bimodal 与 chooser 打包为 4096 项 4-bit 表；相对首个正确的大表
  N5E，route LUT -4.983%、BRAM -6.154%，20 项结果逐项一致。
- 官方功能测试 58/58 PASS，性能测试 20/20 PASS。
- 相对 WB-N4-A，CPU 周期几何平均加速 1.028746x（+2.875%），SoC 周期
  加速 1.017652x（+1.765%），总分支误预测减少 13.003%。
- 10 ns post-route phys-opt WNS +0.010 ns、TNS 0、无布线错误；延迟估算
  Fmax 较 N4 下降 1.291%，结合 CPU 周期后的性能估算仍提升 1.546%。
- route 资源相对 N4：LUT +1.107%、FF +0.059%、BRAM +7.018%、DSP 不变。
- 详细设计与完整百分比报告见 `docs/WB-N5_DESIGN.md` 和
  `docs/WB-N5_REPORT.md`。

## WB-N4-A（DISPATCH-valid 扇出控制，2026-07-29）

- 仅对超过 500 个负载的 DISPATCH valid 寄存器添加 `max_fanout=64`，
  不改变功能逻辑和流水级。
- bitcount、CoreMark PASS 且 CPU Count 与 WB-N2 完全一致。
- route LUT -0.319%、FF +0.307%、10 ns WNS +5 ps，延迟估算 Fmax
  +0.051%。
- 结论：保留为小幅正收益 checkpoint，不包装成主要性能升级。详见
  `docs/WB-N4_REPORT.md`。

## WB-N2-B（初赛翻译快路径，2026-07-29）

- 新增 N2 双配置地址翻译前端：N1 保留完整 TLB/MMU，N2 保留 DA 与
  DMW0/DMW1，生成期移除初赛不使用的 16 项全关联 TLB 取指/访存查找。
- N2-A 的纯 DA 裁剪虽然 bitcount PASS，但因丢失 DMW cached 属性使 CPU
  周期从 25,114 恶化到 578,819；最终 N2-B 已修复并将该实验记录为消融。
- N2-B 官方 bitcount 为 25,114 CPU 周期，CoreMark 为 404,385 CPU 周期，
  均与 WB-N1 逐周期一致。
- 最终综合 LUT 为 43,953，相对 WB-N1 减少 2,836（-6.06%）。
- 最终 route LUT 为 43,516，相对 WB-N1 减少 2,875（-6.20%）。
- 10.000 ns post-route WNS 为 +0.134 ns，正式通过 100 MHz；WB-N1 同约束
  为 -0.085 ns。
- 现有布线在 9.90 ns 静态重签核通过（+0.034 ns），9.85 ns 失败
 （-0.016 ns）；该数据只用于边界估算。

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
