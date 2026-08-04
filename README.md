# Longxin / WeBattle-Core WB-N14

面向 NSCSCC 2026 团体赛初赛环境的 LoongArch32 Reduced 乱序多发射处理器。
当前冻结版本为 `WB-N14-20PASS`。

本项目是 [NOP-Core](https://github.com/NOP-Processor/NOP-Core) 的明确派生作品，
不是从零实现。上游固定提交为
`1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`，许可证为 MIT；原始版权、
许可证和本项目修改边界均保留并公开说明。

## 封版结果

| 指标 | WB-N14 |
|---|---:|
| 官方功能仿真 | 58 / 58 PASS |
| 官方性能仿真 | 20 / 20 PASS |
| 相对同环境 OpenLA500 周期几何平均加速 | 1.9654x |
| 相对同环境 OpenLA500 周期总和加速 | 2.5329x |
| CPU 核级布局布线 | 100 MHz，WNS +0.006 ns，TNS 0 |
| 完整 SoC 应急签核 | CPU 32.727 MHz，WNS +0.978 ns，WHS +0.052 ns |
| 独立组件加权覆盖 | 2,428 / 4,791 = 50.68% |

完整 SoC 使用 Vivado 2025.2 生成候选 bitstream，路由错误和 bitstream DRC
均为 0。赛事指定的 Vivado 2023.2 重建与目标板稳定性测试尚未完成，因此上述
仿真周期不得当作官方实板成绩。

## 微架构

- 4 路取指、3 路译码/重命名、最多 5 个执行通道、最多 3 路顺序退休；
- 32 项 ROB、物理寄存器重命名、年龄优先发射、旁路唤醒与精确恢复；
- tournament 方向预测、BTB、推测全局历史和检查点 RAS；
- 两路 4 KiB I-Cache、两路 4 KiB D-Cache、Store Buffer 与 AXI 主接口；
- CSR、异常、中断和 TLB/MMU；初赛配置支持 DA/DMW 快速路径。

## 目录

- `src/`：SpinalHDL/Scala 处理器实现；
- `integration/`：NSCSCC `core_top` 集成包装；
- `generated/WB-N14/`：仿真/提交用生成 Verilog；
- `generated/WB-N14-synth/`：综合观测裁剪版生成 Verilog；
- `reports/WB-N14/`：58/58、20/20、PPA 与来源覆盖证据；
- `docs/`：架构、版本演进和独立组件边界；
- `harness/`：生成、对比和布局布线脚本。

## 生成

需要 JDK 11 和 sbt 1.9.9：

```powershell
java -jar sbt-launch-1.9.9.jar "runMain NOP.WeBattleMain N14"
java -jar sbt-launch-1.9.9.jar "runMain NOP.WeBattleMain N14 synth"
```

提交集成使用 `integration/core_top.sv`、生成的 `wb_raw_top.v` 和
`xilinx_ip/multiplier.xci`。详细结果见
[`reports/WB-N14/WB-N14_RELEASE_REPORT.md`](reports/WB-N14/WB-N14_RELEASE_REPORT.md)。

## 封版指纹

- 仿真/提交 `wb_raw_top.v`：
  `58BA10635A9E575AF5C393705AC96A68ED873E658A37AA3BA589869813149F7F`
- 综合观测裁剪版 `wb_raw_top.v`：
  `4180578FF78A01A1A65E2C22DB046FB42FDB744EDFFB1918929C92063E308A53`

## 合规边界

1. 保留 NOP-Core MIT 许可证和上游作者版权；
2. 不把继承的流水线框架、AXI 类型、RAM primitive 或 Xilinx IP 声明为本队原创；
3. 独立组件覆盖率按冻结接口和验证门禁统计，不以重命名或文本 churn 代替；
4. 只有实测数据才能写入赛事成绩表，仿真结果不能冒充上板结果。

更多内容见 [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)、
[`docs/WB_COMPONENT_OWNERSHIP.md`](docs/WB_COMPONENT_OWNERSHIP.md) 和
[`CHANGELOG.md`](CHANGELOG.md)。
