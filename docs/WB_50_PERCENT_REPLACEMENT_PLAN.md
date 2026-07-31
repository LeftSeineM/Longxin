# WeBattle 50% core-replacement plan

Status: **target reached by accepted WB-N14 at 50.68%**.

Upstream remains NOP-Core commit
`1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495` under the MIT license.
Attribution and upstream notices must remain in every release.

## 1. What “50%” means

The primary metric is **owned microarchitecture coverage**, not renamed files
or raw textual churn.

The denominator is the 4,791 nonblank code lines in the frozen upstream
`pipeline/fetch`, `pipeline/decode`, `pipeline/core`, `pipeline/exe`, and
`pipeline/mem` domains.  Builder utilities, ISA constants, wrappers,
blackboxes, and privileged architecture are excluded because rewriting those
only to increase a percentage would add risk without creating a stronger CPU.

A module contributes its frozen-upstream code-line weight only after:

1. a WeBattle implementation is written from a documented interface contract;
2. the upstream implementation remains available as an elaboration-time A/B
   fallback until validation is complete;
3. the new path passes the official 58 functional and 20 performance tests;
4. routed timing, resource use, and cycle changes are recorded;
5. the ownership matrix identifies inherited, modified, and replaced parts;
6. the implementation is not a mechanical rename, formatting rewrite, or
   copied body with superficial edits.

The target is at least **2,396 / 4,791 weighted lines (50.0%)**.  Physical new
code lines and upstream textual churn are reported as secondary audit metrics,
but neither is allowed to substitute for component ownership.

## 2. Release policy

Small fixes do not receive a new N-number.  One N-number represents a coherent
module cluster and is promoted only after all validation gates pass.

Two heads are tracked:

- **performance head**: fastest fully validated version;
- **ownership head**: highest-coverage version that still passes timing and all
  official tests.

The submission candidate must eventually merge both.  This prevents a large
replacement experiment from being advertised as a performance upgrade when it
is slower.

## 3. Replacement batches

The weights below are frozen-upstream nonblank lines covered by the named
component boundaries.  They are planning weights, not claims that the new
implementation must have the same line count.

| Batch | Coherent replacement scope | Weight | Cumulative planned coverage |
|---|---|---:|---:|
| Accepted/WIP frontend+integer datapath | Fetch queue, checkpointed RAS, integer ALU/compare/branch datapath | 300 | 6.3% |
| N9 backend state cluster | Rename/RAT, physical-register state, ROB storage, retirement/commit control | 688 | 20.6% |
| N10 integer scheduling checkpoint | Integer issue-queue state and compaction | 96 | 22.6% |
| N11 MulDiv scheduling/execute | MulDiv queue plus MulDiv execution control | 242 | 27.7% |
| N12 memory scheduling | Memory issue-queue state and compaction | 119 | 30.2% |
| N13 LSU/control cluster | Address generation, load/store execution, store buffer, load post-process and uncached control | 515 | 40.9% |
| N14 instruction-front-end cluster | I-cache and BTB control/data paths | 468 | **50.7%** |

This route reaches the target without rewriting decode tables merely to gain
line count and without making the large D-cache the critical path to 50%.
Decoder, D-cache and PC remain reserve batches if an earlier subcomponent must
be rejected for timing or correctness.

## 4. Performance constraints

Every batch must report:

- functional pass count out of 58;
- performance pass count out of 20;
- CPU-cycle and SoC-cycle geometric means;
- per-test faster/tie/slower counts;
- routed WNS/TNS at the official constraint;
- route-derived Fmax estimate;
- LUT, FF, BRAM, and DSP use;
- combined `Fmax / cycles` estimate against both upstream NOP-Core and the
  current performance head.

Negative WNS cannot be promoted.  A coverage batch that passes functionality
but loses performance stays an experimental checkpoint until redesigned.

## 5. Immediate execution order

1. Preserve N10 as the strict performance head and N14 as the accepted 50.68%
   ownership head.
2. Preserve the N11 MulDiv, N12 memory-scheduling, N13 LSU and N14 front-end
   packages so every
   replacement remains individually attributable and reversible.
3. Treat N13 as closed: five independently selectable LSU/control boundaries
   passed 58/58, 20/20 and routed WNS +0.020 ns without cycle regression.
4. Treat N14 as closed: its I-cache and predictor/BTB boundaries passed 58/58,
   20/20 and routed WNS +0.006 ns, reaching 50.68% accepted coverage.
5. Keep N14's two sub-boundaries independently selectable and preserve N13 as
   the direct fallback. Further work should first improve N14's route margin,
   then merge performance changes only when they beat N10 after routing.
6. Keep the previous validated generated RTL package at every accepted
   checkpoint and update the ownership numerator only after all gates pass.
