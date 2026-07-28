# WB-N1 工程基线报告

日期：2026-07-29

## 1. 结论

WB-N1 已经形成第一版可独立维护、可重新生成、可接入2026官方SoC的
NOP-Core派生工程。它不是1.5倍性能版，也还不是可直接提交的候选版。

当前确凿结论：

- 最终bitcount与CoreMark均PASS，CPU周期与冻结NOP基线相同；
- xc7a200t-2布局布线在98.04 MHz下WNS为+0.115 ns、TNS为0；
- 相同约0.115 ns裕量下，旧NOP包装通过95.24 MHz，WB-N1通过98.04 MHz；
- 两个已复测负载周期不变，因此当前可证明的综合性能提升约为1.0294倍；
- 100 MHz约束下WNS为-0.085 ns，不能宣称100 MHz时序通过；
- 完整58项功能、20项性能和上板回归尚未在最终WB-N1上完成。

## 2. 来源与边界

- 上游：`NOP-Processor/NOP-Core`
- 上游提交：`1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`
- 许可证：MIT，原许可证保留在仓库根目录
- WB-N0：上游微架构、2026兼容层与官方包装的冻结基线
- WB-N1：本队维护的原生顶层、配置入口、观测/综合双构建和实验框架

答辩时必须明确说明派生关系。更名、改端口或重新排版都不构成原创微架构；
本队贡献应以修改清单、实验数据、验证证据和后续实质性架构改进来说明。

## 3. 最终配置

| 项目 | WB-N1 |
|---|---:|
| ISA | LA32R，含2026启动所需CPUCFG兼容 |
| 执行方式 | 乱序执行、顺序提交 |
| 取指/译码/提交 | 4 / 3 / 3 |
| 执行端口 | 3整数 + 1乘除 + 1访存 |
| ROB | 32项 |
| I-Cache / D-Cache | 2路4 KiB / 2路4 KiB |
| BTB / PHT / RAS | 1024 / 8192 / 8 |
| 默认方向预测 | 上游gselect、弱不跳转初值 |
| 总线 | AXI |

## 4. 本版实质性改动

1. 新增 `WeBattleRawTop` 和2026 `core_top`，建立稳定提交边界。
2. 新增 `WeBattleProfiles.N0/N1`，所有实验配置可重复生成。
3. 增加CPUCFG兼容，保证2026性能程序可以启动。
4. 建立仿真观测版与综合净化版双构建。
5. 增加提交、双/三提交、访存、分支、误预测和Cache miss计数。
6. 实现可切换bank-safe gshare和PHT初值，用实测决定是否进入默认配置。
7. 建立RTL编译、官方SoC回归、综合、层次面积、route和任意周期重签核脚本。

## 5. 被否决的实验

| 实验 | 结果 | 决策 |
|---|---|---|
| bank-safe gshare | bitcount 25,114→30,255；误预测1,013→1,667 | 默认关闭 |
| PHT弱跳转初值 | bitcount 25,114→26,217；误预测1,013→1,060 | 默认回退弱不跳转 |
| RAS 8→16 | CoreMark周期无收益；综合LUT最高增至50,613 | 回退8项 |

这些实现保留为后续参数扫描入口，但不进入WB-N1最终配置。

## 6. 官方SoC回归

| 负载 | 结果 | CPU周期 | 关键诊断 |
|---|---:|---:|---|
| bitcount | PASS | 25,114 | commit 48,368；I$ miss 282；D$ miss 56；误预测1,013 |
| CoreMark | PASS | 404,385 | 2.472891 CoreMark/MHz；commit 392,530；误预测11,457 |

性能计数器从复位开始计数，而官方CPU周期使用程序自己的测量窗口，因此不能在
所有短程序上直接用两者相除声称精确CPI。CoreMark中该诊断比值约为
404,385 / 392,530 = 1.0302 CPI，仅作为定位依据。

证据：

- `D:/NSCSCC2026/PulseLA/build/wb_n1_final_bitcount/official_bitcount.log`
- `D:/NSCSCC2026/PulseLA/build/wb_n1_final_coremark_retry/official_coremark.log`

## 7. 资源与时序

### 7.1 综合资源

| 指标 | 旧NOP包装 | WB-N1最终综合 | 变化 |
|---|---:|---:|---:|
| LUT | 43,678 | 46,789 | +3,111（+7.12%） |
| FF | 16,893 | 16,980 | +87（+0.52%） |
| BRAM Tile | 28.5 | 28.5 | 0 |
| DSP | 4 | 4 | 0 |

层次差异主要集中在ROB和FetchBuffer的综合映射。该结果不是功能结构扩宽的
性能收益证明；它反映了新顶层可观察点和Vivado对多端口RAM/宽多路器的不同映射。

### 7.2 最终布局布线

| 设计/约束 | WNS | TNS | 状态 |
|---|---:|---:|---|
| WB-N1，10.000 ns（100 MHz） | -0.085 ns | -0.251 ns | 未通过 |
| WB-N1，10.200 ns（98.04 MHz） | +0.115 ns | 0 | 通过 |
| 旧NOP，10.500 ns（95.24 MHz） | +0.117 ns | 0 | 通过 |

关键路径口径：

- 旧NOP：约10.383 ns，理论临界约96.31 MHz；
- WB-N1：约10.085 ns，理论临界约99.16 MHz；
- 相近正裕量下稳定频率提升：
  `98.039 / 95.238 = 1.0294`，约+2.94%。

由于已复测负载周期不变，当前可证明的“频率×周期”提升同为约1.0294倍。
这距离1.5倍目标仍很远。

证据：

- `reports/WB-N1/synth_final/utilization_synth.rpt`
- `reports/WB-N1/synth_final/utilization_hier.rpt`
- `reports/WB-N1/route_final/timing_route_10ns.rpt`
- `reports/WB-N1/route_final/timing_route_10p2ns.rpt`
- `reports/WB-N1/route_final/timing_nop_old_10p5ns.rpt`
- `reports/WB-N1/route_final/post_route.dcp`

## 8. 版本门禁状态

| 门禁 | 状态 |
|---|---|
| Scala生成N0/N1 | PASS |
| 仿真版与综合版xvlog | PASS |
| 最终bitcount | PASS |
| 最终CoreMark | PASS |
| 98.04 MHz WNS≥0 | PASS |
| 最终58/58 | 待跑 |
| 最终20/20 | 2/20已跑，待完整回归 |
| 上板连续回归 | 待跑 |
| Linux启动 | 尚未开始 |

因此WB-N1应标记为“工程基线”，不能标记为“提交候选版”。

## 9. WB-N2方向

下一版不继续做无数据的小参数微调，优先完成：

1. 关键路径端点分析，针对ROB读出、旁路和Cache信息RAM做寄存切分；
2. 保持3提交能力的前提下，降低ROB多端口RAM和宽选择器成本；
3. 对PHT历史长度、索引方式和BTB组织做20负载扫描，不再用单一bitcount定方案；
4. 统计双/三提交占比、issue利用率和各类stall，确定IPC未兑现的首要原因；
5. 每项修改依次过bitcount、CoreMark、58/58、20/20和route，失败立即回退。
