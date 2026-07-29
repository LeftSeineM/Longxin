# WB-N2 关键创新：面向比赛阶段的双配置地址翻译前端

日期：2026-07-29

## 1. 结论

WB-N2 把同一套乱序核拆成两个可重复生成的硬件配置：

- `N1`：保留完整 TLB/MMU，供 Linux 和决赛扩展使用；
- `N2`：保留直接地址模式与 DMW0/DMW1，移除初赛负载不使用的
  16 项全关联 TLB 取指/访存查找。

这不是把地址强制设为 cached，也不是删除整个特权架构。N2 仍遵守 CRMD 的
DA/PG/MAT 属性、支持两个直接映射窗口，并在页模式地址未命中 DMW 时产生
TLBR；被裁掉的是每次取指和访存都会计算、但初赛程序不会消费的全关联页表查找。

当前实测结论：

- 官方 bitcount 与 CoreMark 均 PASS，CPU 周期与 WB-N1 逐周期一致；
- 最终综合 LUT 减少 2,836（-6.06%）；
- 最终布局布线 LUT 减少 2,875（-6.20%）；
- 10.000 ns 布局布线 WNS 从 WB-N1 的 -0.085 ns 提升到 +0.134 ns；
- 正式通过频率由 98.04 MHz 提升到 100 MHz，已测负载综合性能约
  `1.0200x`；
- 同一 post-route 结果在 9.90 ns 静态重签核时 WNS 为 +0.034 ns，
  在 9.85 ns 时为 -0.016 ns，边界估算约 101.36 MHz。该估算不等同于
  更紧约束下重新布局布线，正式保障值仍为 100 MHz。

WB-N2 还不是提交候选版：完整 58/58、20/20、官方完整 SoC 综合与上板回归
尚未完成。

## 2. 问题定位

WB-N1 的 100 MHz post-route 关键路径之一为：

`IF1 PC register -> 16-entry associative TLB compare/select -> IF2 physical address`

该路径数据延迟约 10.095 ns，共 13 级逻辑，其中约 74.79% 是布线延迟。
官方初赛性能环境不使用页表映射，但原设计仍在每次取指和访存时并行计算
TLB 查询，因此为未使用能力持续支付面积、扇出和时序代价。

优化后，TLB 路径退出前 20 条关键路径；10 ns post-route 最差路径转移为
`Rename/Dispatch -> ROB`，数据延迟 9.728 ns。

## 3. 数据通路

### 3.1 N1 完整配置

```text
virtual address
  +-> direct address (DA)
  +-> DMW0 / DMW1 compare
  +-> 16-entry associative TLB compare/select
       |
       +-> mode-dependent result mux -> physical address/cache attribute
```

### 3.2 N2 初赛配置

```text
virtual address
  +-> DA path ------------------------------+
  +-> DMW0 / DMW1 compare -> direct result -+-> physical address/cache attribute
  +-> mapped and no DMW hit -> TLBR

  (no per-access associative TLB lookup is elaborated)
```

配置在 Scala elaboration 阶段生效，所以综合输入中不存在被常量门控但仍占用
关键路径的 TLB 查找网络。完整 TLB 寄存器和特权指令能力仍由 N1 配置保留。

## 4. 关键实现

| 文件 | 改动 |
|---|---|
| `src/MyCPUConfig.scala` | 增加 `TranslationConfig.contestDirectMode` 与 `N2` 配置 |
| `src/WeBattleRawTop.scala` | 增加 `N2` 生成入口 |
| `src/pipeline/core/MyCPUCore.scala` | 向取指翻译插件传递配置 |
| `src/pipeline/fetch/InstAddrTranslationPlugin.scala` | N2 保留 DA/DMW，裁掉取指 TLB 查找 |
| `src/pipeline/mem/AddressGenerationPlugin.scala` | N2 保留 DA/DMW，裁掉 LSU TLB 查找 |

## 5. 消融实验

第一版 N2-A 同时旁路了 TLB 与 DMW。它可以通过 bitcount 功能检查，但把官方
运行时的 DMW cached 属性丢失，导致程序以 uncached 方式运行：

| 版本 | bitcount | CPU Count | 结论 |
|---|---:|---:|---|
| WB-N1 | PASS | 25,114 | 基线 |
| N2-A：DA only | PASS | 578,819 | 失败，慢约 23.05 倍 |
| N2-B：DA + DMW fast path | PASS | 25,114 | 采用 |

这个结果证明不能用“所有地址直通”替代架构语义。最终 N2-B 只删除全关联 TLB，
完整保留 DMW 地址与 MAT/cache 属性。

## 6. 官方负载验证

| 负载 | WB-N1 CPU Count | WB-N2 CPU Count | 状态 | 周期变化 |
|---|---:|---:|---:|---:|
| bitcount | 25,114 | 25,114 | PASS | 0 |
| CoreMark | 404,385 | 404,385 | PASS | 0 |

bitcount 的 N2 诊断计数为：

- commit：48,368
- I-Cache miss：282
- D-Cache miss：56
- branch：10,549
- branch mispredict：1,013

由于两个负载周期完全相同，N2 当前不宣称 CPI/IPC 改善。实测性能收益来自
可实现频率提高。

## 7. PPA 对比

### 7.1 综合

| 指标 | WB-N1 | WB-N2 | 变化 |
|---|---:|---:|---:|
| LUT | 46,789 | 43,953 | -2,836（-6.06%） |
| FF | 16,980 | 16,940 | -40（-0.24%） |
| BRAM Tile | 28.5 | 28.5 | 0 |
| DSP | 4 | 4 | 0 |
| 10 ns WNS | 负裕量 | +0.280 ns | 通过 |

### 7.2 布局布线

| 指标 | WB-N1 | WB-N2 | 变化 |
|---|---:|---:|---:|
| LUT | 46,391 | 43,516 | -2,875（-6.20%） |
| FF | 16,993 | 16,954 | -39（-0.23%） |
| BRAM Tile | 28.5 | 28.5 | 0 |
| DSP | 4 | 4 | 0 |
| 10.000 ns WNS | -0.085 ns | +0.134 ns | +0.219 ns |
| 已验证通过频率 | 98.04 MHz | 100 MHz | +2.00% |

按两个已验证负载周期不变计算：

`N2 / N1 = 100 / 98.04 = 1.0200x`

这是当前可复现、可审计的性能结论，不是 1.5 倍目标已经完成。

## 8. 复现

生成时必须使用 JRE 11。当前机器可使用：

```powershell
$env:COURSIER_CACHE='D:\NSCSCC2026\OpenSource-Cores\tools-cache\coursier'
& 'D:\Software\2025.2\tps\win64\jre11.0.16_1\bin\java.exe' `
  '-Dsbt.boot.directory=D:\NSCSCC2026\OpenSource-Cores\tools-cache\sbt-boot' `
  '-Dsbt.global.base=D:\NSCSCC2026\OpenSource-Cores\tools-cache\sbt-global' `
  '-Dsbt.ivy.home=D:\NSCSCC2026\OpenSource-Cores\tools-cache\ivy2' `
  -jar 'D:\NSCSCC2026\OpenSource-Cores\tools\sbt\sbt-launch-1.9.9.jar' `
  'runMain NOP.WeBattleMain N2' `
  'runMain NOP.WeBattleMain N2 synth'
```

生成结果：

- 仿真：`build/wb-n2/wb_raw_top.v`
- 综合：`build/wb-n2-synth/wb_raw_top.v`
- 已集成版本：`generated/WB-N2`、`generated/WB-N2-synth`

关键报告：

- `reports/WB-N2/official`
- `reports/WB-N2/synth_dmw`
- `reports/WB-N2/route_dmw`

## 9. 后续方向

N2 已把地址翻译从关键路径移除。下一阶段应针对新的真实瓶颈推进：

1. Rename/Dispatch 到 ROB 的高扇出与长布线；
2. Issue Queue 唤醒/选择和 MULDIV issue-slot 传播；
3. 分支误预测恢复延迟；
4. 完整 20 项性能测试中的 I$/D$ miss 与 no-commit 周期。

这些优化必须分别做功能、周期和 post-route 消融，不能用单一程序或综合估计
替代官方全套结果。
