# WB-N12 memory scheduling replacement

Status: **accepted ownership checkpoint**.

Baseline: `WB-N11-20PASS`.

## Accepted boundary

N12 replaces the inherited memory issue-queue state transition with
`WeBattleMemIssueQueueState`. The memory scheduler is a five-entry,
oldest-only dense FIFO rather than a general reservation station. The accepted
implementation owns:

- the single valid/invalid insertion-boundary calculation;
- dense multi-lane append and registered-capacity backpressure;
- accepted-head removal and exact one-slot compaction;
- operand wakeup folding into the final destination slots;
- atomic queue flush and density/lane-order assertions.

Dispatch decode, physical-register broadcasts, address generation, the LSU,
store buffer, cache datapaths and AXI control remain inherited or separately
integrated. They receive no N12 ownership credit.

## Behavioral contract

The queue deliberately does not borrow a slot released by a same-cycle issue.
Capacity is derived from registered state, matching the accepted N11 dispatch
contract. Push lanes must be dense, memory operations issue only from the
oldest entry, and a flush has final priority over append, shift and wakeup.

This contract preserved every official benchmark cycle exactly while replacing
the queue's state owner with a smaller specialized implementation.

## Physical implementation

The memory-completion permit feeds physical-register readiness and several
reservation-station wakeup paths. N12 adds a profile-scoped `max_fanout=64`
implementation hint to this signal. The hint permits Vivado to replicate its
driver without inserting an architectural register or changing same-cycle
wakeup behavior. It is timing guidance and is not counted as an independently
owned microarchitecture component.

Three rejected physical variants remain documented in the development
evidence:

- the first generic state-engine reuse routed at WNS -0.275 ns;
- the specialized FIFO without the fanout hint routed at WNS -0.245 ns;
- a balanced wakeup-OR experiment grew LUT use and routed at WNS -1.262 ns.

The accepted specialized FIFO plus fanout guidance reached WNS +0.010 ns after
post-route physical optimization.

## Validation

| Gate or metric | N12 |
|---|---:|
| Official functionality | **58/58 PASS** |
| Official performance | **20/20 PASS** |
| CPU-cycle geometric mean | **172,821.02** |
| SoC-cycle geometric mean | **194,719.81** |
| Cycles versus N11 | **identical on 20/20** |
| Routed WNS / TNS | **+0.010 ns / 0** |
| Route errors | **0** |
| Route-derived Fmax | **100.100 MHz** |
| LUT / FF | **43,968 / 20,400** |
| BRAM / DSP | **30.5 / 4** |
| Combined score versus N11 | **1.000100x (+0.010%)** |
| Combined score versus N10 | **0.997325x (-0.268%)** |

N12 is the ownership head and is marginally faster than N11 by the routed
estimate. N10 remains the strict performance head.

## Ownership

The frozen memory-scheduling boundary weight is 119 lines:

`(1,326 + 119) / 4,791 = 30.16%`

The independently written source audit now contains 14 `WeBattle*.scala`
files and 1,458 nonblank lines. Raw churn is reported separately and is not
used as an originality claim.
