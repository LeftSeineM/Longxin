# WB-N5 design: packed tournament branch predictor

Status: accepted engineering checkpoint; 58/58 functional, 20/20 performance
and 100 MHz implementation timing gates passed.

## 1. Promotion contract

WB-N5 is a performance release derived from the frozen
`WB-N4-A-20PASS` baseline.  It is promoted only after:

1. official performance tests pass 20/20;
2. the official functional suite passes;
3. routed WNS is non-negative at the declared clock;
4. the frequency-and-cycle score is better than N4;
5. source provenance and the complete percentage comparison are recorded.

Passing RTL generation alone is not a release result.

## 2. Motivation

N4 uses a bank-safe global-history predictor.  It works well on branches
whose behavior correlates with recent branch history, but a single global
scheme is vulnerable to aliasing and performs poorly on branches that are
better described by their own PC-local bias.

N5 predicts every fetched instruction with two predictors in parallel:

- the existing global-history predictor;
- a PC-indexed bimodal predictor.

A PC-indexed 2-bit chooser selects the source.  The chooser changes only when
the two predictors disagree and exactly one predictor is correct.  This
preserves stable decisions and lets individual branch PCs learn whether local
bias or global correlation is more useful.

## 3. Microarchitecture

```text
                         ┌─ global PHT (PC + GHR) ── counter ─┐
fetch PC + speculative GHR                              chooser ── prediction
                         └─ packed PC table ─ bimodal counter ┘
                                             chooser counter ──┘
```

Both the bimodal and chooser counters use the same PC address and are read and
written together.  The compact implementation therefore packs them into one
4-bit RAM word and uses 4096 PC entries:

```text
packed word = {chooser[1:0], bimodal[1:0]}
```

Packing the two fields is behaviorally equivalent for a given entry.  Halving
the PC-only entry count can introduce additional aliasing, so its cycle counts
are revalidated rather than assumed equal.  Together these changes remove one
entire banked RAM structure.  This is important because the
first correct tournament implementation improved branch-heavy cycle counts
but consumed four extra BRAM tiles and produced negative routed WNS at
100 MHz.  The packed representation targets half that extra BRAM cost and
less placement congestion.

## 4. Prediction and training

At fetch:

1. read the global, bimodal and chooser counters in parallel;
2. choose global when the chooser MSB is one, otherwise choose bimodal;
3. use the selected counter MSB as taken/not-taken;
4. carry all recovery counters with the instruction.

At branch resolution:

1. saturating-update both direction counters with the real outcome;
2. if global is correct and bimodal is wrong, increment the chooser;
3. if bimodal is correct and global is wrong, decrement the chooser;
4. leave the chooser unchanged when both agree or are both wrong;
5. train with the original prediction-time PC/GHR and counter snapshot.

Calls and returns keep the existing strongly-taken initialization behavior.
Speculative GHR update and recovery remain those of N4.

## 5. Rejected N5 cache experiments

Before selecting the tournament predictor, four D-cache experiments were
measured and rejected:

- line-buffer install-copy: correct, but all selected cycle counts regressed;
- direct-hit stream buffer: one large speedup but failed `inner_product`;
- 128-byte cache lines: raised a write-back exception;
- early restart: negligible `bitcount` gain and a large `stream_copy`
  regression.

None of those changes is part of the final N5 source.  The D-cache is restored
to N4 behavior.  Keeping these failures documented prevents an attractive
single-test result from being mistaken for a valid release.

## 6. Open-source provenance

- Inherited base: NOP-Core, upstream commit
  `1a5986d9d1ff02d2156fb4d065e5ad0ba0f94495`, MIT license.
- Competition baseline: WeBattle `WB-N4-A-20PASS`.
- Tournament prediction is a standard published microarchitectural concept.
  The N5 SpinalHDL implementation was written for this repository.
- No RTL/Scala source lines were copied from another tournament predictor.

The release report separately states inherited code, N5 source churn, and
measured performance.  Conceptual similarity is not presented as original
invention.

## 7. Validation outputs

The sign-off report must include:

- per-program N4/N5 CPU and SoC cycles and percentage change;
- committed instructions, branch instructions and mispredictions;
- geometric-mean cycle speedup;
- synth/route WNS, estimated Fmax and LUT/FF/BRAM/DSP deltas;
- combined `Fmax / geometric-mean-cycles` speedup;
- functional and performance pass counts;
- source additions, deletions and percentage churn.

The authoritative release status is recorded in `docs/WB-N5_REPORT.md`;
intermediate measurements are not submission claims.
