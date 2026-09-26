# WB-N14 public deliverables

公开发布版本：`WB-N14-PUBLIC-20260926`

下载地址：
[GitHub Release](https://github.com/LeftSeineM/Longxin/releases/tag/WB-N14-PUBLIC-20260926)

## 版本结论

公开主线采用最后一个完整通过门禁的 `WB-N14-20PASS`：

- 官方功能仿真：58/58 PASS；
- 官方性能仿真：20/20 PASS；
- CPU核级布局布线：100 MHz，WNS +0.006 ns，TNS 0；
- 完整SoC候选：CPU 32.727 MHz，WNS +0.978 ns，WHS +0.052 ns；
- 独立组件加权覆盖：50.68%；
- 相对同环境OpenLA500的20项周期几何平均加速：1.9654x。

开发目录中晚于N14标签的内容只有被否决的时序探索、未提交实验和生成缓存，
没有形成通过58/58、20/20和物理门禁的新版本，因此未将其冒充为新发布版本。

## Release附件

### `WB_N14_VIVADO_2023_MIGRATION_FULL.zip`

完整复现/迁移包，约359.63 MiB，包含：

- WB-N14源代码和生成RTL；
- 官方SoC RTL、testbench和DDR模型；
- 58项功能测试源码及预编译镜像；
- 20项性能测试源码及预编译镜像；
- Vivado工程元数据、XCI、XDC、编译配置；
- 2025.2成功工程的DCP、bitstream、日志和报告；
- Vivado 2023.2迁移说明、XPR文件集清单和逐文件SHA-256清单。

SHA-256：
`388F20B88EDA73EBCB7811F4C2D4DAACC86FAA76946A237533759DD82ACCCF07`

### `SCHOOL_1_CAPTAIN_DRAFT_DO_NOT_UPLOAD.zip`

4.31 MiB的赛事提交结构候选，包含源码、设计报告和候选bitstream。
文件名和成绩表仍含身份占位符，且缺少Vivado 2023.2与实板复核，因此不得原样冒充正式提交。

SHA-256：
`49DDB1AE2D401C7CD0B43CC28B5F708113A415AE3415FCF0523B8709CBFA1DF7`

### 其他附件

- `WB-N14-source.zip`：冻结源码归档；
- `WB-N14-design-report.pdf`：六页设计与验证报告；
- `soc_top_func_candidate_vivado2025_2.bit`：功能候选bitstream；
- `soc_top_perf_candidate_vivado2025_2.bit`：性能候选bitstream；
- `FULL_SOC_SIGNOFF_2025_2.md`：完整SoC签核边界；
- `*.sha256`：大包与候选提交包校验值。

两个候选bitstream当前使用同一32.727 MHz SoC配置，其SHA-256均为：
`F3B91E9D8F56817C4E041253FB0A2DB5B554297ACA3FA1E84982AB2E58779FE9`

## 重要边界

1. 项目明确为NOP-Core的MIT许可派生作品，保留上游版权和许可证；
2. 58/58和20/20是官方仿真环境结果，不是实板成绩；
3. 完整SoC签核使用Vivado 2025.2，不替代赛事指定的2023.2复核；
4. 提交成绩表只能填写真实板测数据，不得用仿真周期冒充。
