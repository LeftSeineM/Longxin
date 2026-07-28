# WB-N1 架构与设计方向

## 1. 设计目标

WB-N1 的目标不是在文件名上“去 NOP 化”，而是在可追溯的上游基础上建立本队
可解释、可测量、可继续演进的高性能内核：

1. 完整兼容 NSCSCC 2026 官方功能与性能程序。
2. 以 `Fmax / 官方周期数` 为最终性能口径。
3. 保持乱序多发射底座，同时优先改善控制流、存储墙和物理时序。
4. 每一版都能从冻结点重新生成、仿真、综合和回退。

## 2. 当前数据通路

```text
PC / BTB / gshare / RAS
          |
     4-wide Fetch
          |
      I-Cache / TLB
          |
     Fetch Buffer
          |
  3-wide Decode / Rename
          |
  Dispatch to issue queues
    |       |        |
  INTx3   MULDIV    MEM
    |       |        |
    +--- bypass / speculative wakeup
                |
             32-entry ROB
                |
          3-wide in-order retire
```

“最多五发射”指三个整数通道、一个乘除通道和一个访存通道的总执行端口数；
受指令相关、功能单元匹配和队列状态影响，并非每周期必然发射五条。

## 3. WB-N1 相对上游的技术增量

### 3.1 2026原生精简顶层

上游 `MyCPU` 面向开发期 Difftest，导出了大量物理寄存器、RAT、TLB、Cache 和
差分验证状态。WB-N1 的 `WeBattleRawTop` 直接实例化微架构核心，只输出：

- AXI主接口；
- 三路退休记录；
- 异常摘要；
- Cache miss与退休分类事件。

这降低生成文件规模，使调试观测与提交硬件的端口边界更清晰。最终综合报告仍以
Vivado实际优化结果为准，不能仅凭源代码端口数量推断面积。

### 3.2 Bank-safe gshare

上游PHT索引采用全局历史与部分PC直接拼接。WB实验实现用PC索引与移位后的GHR异或，
但保留最低两位PHT bank选择不变。因此一次四指令取指仍可连续读取四个bank，
不会为了gshare引入bank冲突。

首轮bitcount实测中，该gshare索引把误预测从1,013增加到1,667，CPU周期从
25,114增加到30,255，因此未进入N1默认配置。实现作为可切换实验项保留；
N1当前继续使用gselect和8项RAS。

16项RAS也已经完成实验：CoreMark周期与8项配置相同，但RAS恢复指针从3位扩为4位后，被逐指令携带进FetchBuffer和ROB，综合LUT从约43.7k增至50.6k。该修改不满足收益/成本条件，已经从N1默认配置撤回。

弱跳转PHT冷启动也在bitcount上产生退化：误预测由1,013增加到1,060，CPU周期
由25,114增加到26,217。因此N1默认保持上游弱不跳转初值，只保留参数化能力。

### 3.3 性能事件

WB-N1公开下列仿真计数器：

- `cycle_counter`
- `commit_inst_counter`
- `icache_miss_counter`
- `dcache_miss_counter`
- `mem_inst_counter`
- `br_inst_counter`
- `br_pre_counter`
- `br_pre_error_counter`
- `dual_commit_cycle`
- `triple_commit_cycle`

这些计数不参与功能，只用于解释CPI、Cache和分支瓶颈。

为了避免退休分类事件迫使综合器保留ROB中的调试字段，生成流程区分：

- 仿真观测版：导出全部事件并实例化计数器；
- 提交综合版：编译期移除事件出口，包装层在 `SYNTHESIS` 下不连接计数器。

两者来自相同Scala源码和N1配置，区别只在非功能观测逻辑。

最终综合表明，移除事件出口并不会单独消除ROB的全部面积差异；不同顶层可观察点
与综合映射会改变多端口RAM和宽多路器的实现。WB-N1相对旧包装增加约7.12%综合LUT，
但最终布线关键路径由约10.383 ns缩短到10.085 ns。完整数字与证据路径见
`docs/WB-N1_REPORT.md`。

## 4. 版本门槛

| 阶段 | 必须满足 |
|---|---|
| 快速门禁 | RTL生成、xvlog、bitcount、CoreMark |
| 功能门禁 | 58/58，零异常、零错误 |
| 性能门禁 | 20/20，每项周期可复现 |
| 物理门禁 | 目标频率WNS >= 0、无布局布线错误 |
| 提交门禁 | 上板连续回归、来源/许可证/修改清单齐全 |

## 5. 后续方向

WB-N1稳定后按收益/风险顺序评估：

1. 分支预测历史长度、BTB组织和RAS命中；
2. ROB、物理寄存器和Issue Queue容量联合扫描；
3. 非阻塞或hit-under-miss Cache路径；
4. Load/Store相关预测与更激进的访存唤醒；
5. 对关键路径进行寄存切分、扇出复制和RAM映射约束；
6. Linux启动所需的特权态、设备一致性和长时间稳定性回归。
