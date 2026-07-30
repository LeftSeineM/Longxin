# WB-N9 backend-state cluster

Status: **accepted ownership checkpoint; 58/58, 20/20 and routed timing pass**.
N9 is the ownership head. N7 remains the strict combined performance head
because N9's cycle reduction is almost exactly offset by lower routed Fmax.

## 1. Scope

N9 is one coherent replacement batch, not four small releases. It replaces
the state and recovery chain that begins at rename and ends at architectural
retirement:

1. speculative and committed register maps plus a three-wide free-register
   ring;
2. physical-register value/readiness update and bypass reduction;
3. explicitly-counted ROB storage with random completion and same-cycle retire
   bypass;
4. ordered retirement, precise recovery and architectural side-effect control.

Each path has an elaboration-time fallback to the attributed NOP-Core
implementation. N7 remains the accepted performance head while N9 is the
ownership-head candidate.

## 2. State contracts

### Rename/free-register state

- Architectural register zero always maps to physical zero.
- Up to three dense decode lanes allocate distinct physical destinations.
- A younger lane observes all older same-group RAW and WAW mappings.
- Commits advance the architectural map and append replaced physical tags to
  the free ring in order.
- Recovery includes same-cycle commits, restores the speculative map to that
  precise committed state, and reclaims all younger allocations atomically.
- Rename does not borrow same-cycle retirement capacity, keeping its ready path
  short and deterministic.

### Physical register state

- Allocation marks a tag busy.
- Completion clears busy and may broadcast a bypass value.
- Every physical register reduces all write, clear and allocate events once;
  issue wakeup and operand reads use the same event set.
- Assertions reject multiple writes or allocations to one physical tag.

### ROB

- The ROB uses explicit `0..32` occupancy rather than pointer phase.
- Three push and three retire lanes obey a dense-prefix contract.
- A full ROB does not borrow same-cycle retire capacity.
- ALU, branch, LSU and MulDiv completion paths write random entries.
- A completion targeting a retiring entry bypasses the state array in the same
  cycle.
- Flush atomically resets head, tail and occupancy.

### Commit/recovery

- Lane zero owns precise exceptions and global side effects.
- Later lanes retire only if the complete prefix is ready and no exception,
  unique-retire or earlier misprediction barrier blocks them.
- Branch recovery, exception recovery, CSR/TLB/cache operations, LL/SC,
  uncached loads, predictor update and architectural register release all meet
  at one ordered retirement boundary.

## 3. Ownership coverage

The frozen upstream weights credited only after all gates pass are:

| Replaced boundary | Upstream weight |
|---|---:|
| Rename/RAT/FreeList | 145 |
| Physical register state | 82 |
| ROB storage/completion | 191 |
| Commit/retirement control | 270 |
| N9 backend-state batch | **688** |

Together with the 300 weighted lines in the fetch queue, RAS and integer
datapath boundaries, accepted ownership is:

`(300 + 688) / 4,791 = 20.62%`

This percentage is component-weighted ownership, not textual churn. The
accepted working tree has 1,101 nonblank lines across eleven independent
`WeBattle*` microarchitecture files. Mechanical upstream-to-working-tree
churn is 2,171 / 8,336 = 26.04%; it is reported for audit only and is not an
originality claim.

## 4. Current evidence

| Gate | Result |
|---|---|
| Scala compile and Spinal elaboration | PASS |
| Official bitcount | PASS |
| bitcount CPU cycles | 23,847 |
| N7 bitcount CPU cycles | 24,396 |
| single-test cycle change | **-2.25%** |
| Official functional 58 | **58/58 PASS** |
| Official performance 20 | **20/20 PASS** |
| CPU-cycle geometric mean | 172,997.81 |
| N7 CPU-cycle geometric mean | 174,999.32 |
| CPU-cycle speedup versus N7 | **1.011570x (+1.157%)** |
| SoC-cycle speedup versus N7 | **1.011040x (+1.104%)** |
| Per-test CPU-cycle result | 19 faster / 0 tie / 1 slower |
| First routed WNS | -0.368 ns |
| Post-route optimized WNS/TNS | **+0.002 ns / 0** |
| Post-route routing errors | 0 |
| Post-route resources | 44,044 LUT / 20,352 FF / 30.5 BRAM / 4 DSP |
| Route-derived Fmax estimate | 100.020 MHz |
| Fmax change versus N7 | **-1.190%** |
| Fmax/cycle combined estimate versus N7 | **0.999530x (-0.047%)** |

N9 improves official cycle efficiency and replaces a large coherent state
cluster, but its added state and recovery fanout consume almost all timing
margin. It is therefore promoted as the ownership head, while N7 remains the
strict combined performance head by about 0.05%.

## 5. Files

- `src/pipeline/decode/WeBattleRenameState.scala`
- `src/pipeline/core/WeBattlePhysicalRegisterState.scala`
- `src/pipeline/core/WeBattleROBStorage.scala`
- `src/pipeline/core/WeBattleCommitPlugin.scala`
- integration switches in `MyCPUConfig`, `RenamePlugin`,
  `PhysRegFilePlugin`, `ROBFIFOPlugin`, and `MyCPUCore`

## 6. Promotion gates

N9 promotion evidence:

1. official functional **58/58 PASS**;
2. official performance **20/20 PASS** with an N7 per-test comparison;
3. WNS **+0.002 ns** after route, with post-route optimization recorded;
4. route status has no failed or unrouted nets;
5. LUT/FF/BRAM/DSP changes are reported;
6. official simulation used `generated/WB-N9`; implementation used the
   synthesis-profile sibling `generated/WB-N9-synth`, generated from the same
   N9 source/configuration state.
