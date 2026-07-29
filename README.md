# WeBattle-Core

WeBattle-Core 是面向 NSCSCC 2026 团体赛开发的 LA32R 乱序多发射处理器。
本项目是 [NOP-Core](https://github.com/NOP-Processor/NOP-Core) 的明确派生作品，
不是从零编写的 CPU，也不会把上游工作描述为本队原创。

上游版本固定为：

- 仓库：`NOP-Processor/NOP-Core`
- 提交：`1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`
- 许可证：MIT，原版权与许可证保留在 `LICENSE`

## 我们维护的版本线

- `WB-N0`：上游微架构 + 2026 `CPUCFG` 兼容 + 官方 SoC 包装，作为冻结基线。
- `WB-N1`：2026 原生精简顶层、完整性能事件计数、可配置bank-safe gshare、
  PHT冷启动策略，是第一版独立维护的派生配置。
- `WB-N2`：初赛专用 DA/DMW 快速翻译配置，保留完整直接映射语义并在生成期
  裁掉未使用的全关联 TLB 查找；Linux/MMU 配置继续由 N1 保留。

## WB-N1 / WB-N2 架构

- LA32R，乱序执行、顺序提交
- 4 路取指、3 路译码、最多 5 个执行通道、3 路退休
- 32 项 ROB、物理寄存器重命名、推测唤醒与旁路网络
- 两路 4 KiB I-Cache、两路 4 KiB D-Cache
- 1024 项 BTB、8192 项 PHT、可配置bank-safe gshare、8 项 RAS
- 精确异常、中断、CSR、TLB、AXI

## 生成

使用仓库外、位于 D 盘的 JDK 11、sbt launcher 和依赖缓存：

```powershell
java -jar sbt-launch-1.9.9.jar "runMain NOP.WeBattleMain N1"
java -jar sbt-launch-1.9.9.jar "runMain NOP.WeBattleMain N2"
```

仿真观测版生成结果位于 `build/wb-n1/wb_raw_top.v`。提交综合版使用
`runMain NOP.WeBattleMain N1 synth`，结果位于
`build/wb-n1-synth/wb_raw_top.v`，它会在生成时移除仿真性能事件出口。
参赛集成还需要
`integration/core_top.sv` 与上游 `xilinx_ip` 中使用的存储器封装。

WB-N1 的实测工程报告见 [docs/WB-N1_REPORT.md](docs/WB-N1_REPORT.md)。
WB-N2 的关键创新、消融和 PPA 实测见
[docs/WB-N2_INNOVATION.md](docs/WB-N2_INNOVATION.md)。
N2 当前已通过 bitcount、CoreMark 和 100 MHz 布局布线时序；完整
58/58、20/20 与上板回归仍是进入提交候选版前的硬门槛。

## 合规原则

1. 始终保留上游 MIT 许可证和作者版权。
2. 文档、答辩和提交清单中明确说明 NOP-Core 来源。
3. 每一个本队修改必须记录动机、代码位置、测试结果和性能变化。
4. 只有通过 58/58、20/20、布局布线 WNS >= 0 和上板回归的版本才可标记为候选提交版。

详细设计见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)，版本变化见
[CHANGELOG.md](CHANGELOG.md)。
