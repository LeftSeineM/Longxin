# WB-N13 LSU and memory-control replacement

Status: **accepted ownership checkpoint; 58/58, 20/20 and routed timing pass**.

Baseline: `WB-N12-20PASS`.

## Goal

N13 is a coherent replacement of the data-cache perimeter rather than another
small patch. It keeps the validated D-cache arrays/refill machinery as a stable
integration anchor while replacing five control/datapath boundaries:

1. effective-address, byte-enable and aligned store-data generation;
2. memory issue, register read, speculative wakeup and ROB/writeback control;
3. store-buffer state, retirement, append, selective flush and pop compaction;
4. byte/half/word load extraction and sign/zero extension;
5. uncached AXI store command and response control.

The planned frozen-upstream weight is 515 lines. If every boundary passes all
gates, accepted coverage becomes:

`(1,445 + 515) / 4,791 = 40.91%`

## Architecture contract

### Address and store alignment

- Effective address is `base + sign-extended immediate`.
- Byte stores select one byte lane; half stores select lanes 0/1 or 2/3; word
  stores select all lanes.
- Half/word alignment exceptions are raised before memory side effects.
- Virtual low address bits remain available for load extraction.
- Contest-direct translation retains the DA/PG/DMW behavior validated by N12;
  the full TLB path remains available in non-contest profiles.

### LSU scheduling and completion

- Only the oldest ready memory queue entry issues.
- Store-buffer reserve capacity covers every in-flight potential producer.
- A retired store-data entry may share a cycle with a compatible store-address
  issue or use an otherwise idle memory issue slot.
- Speculative load wakeup keeps the accepted N12 timing contract; a MEM2 stall
  broadcasts wakeup failure.
- ROB completion and PRF writeback remain atomic with pipeline validity and
  exception state.

### Store buffer

- Entries are dense and program ordered.
- Commit marks exactly the first unretired entry.
- Append uses the single valid/invalid boundary.
- Flush discards only speculative entries and preserves retired stores.
- Pop shifts exactly one entry; flush observes the post-retire/post-append
  staged state.
- Newer cached stores override older bytes during load forwarding.

### Load extraction

One explicit aligner handles byte/half/word extraction. Uncached-load data from
the store-buffer path and ordinary cached-load data use the same extension
datapath.

### Uncached stores

AW and W may handshake independently and exactly once.
The next uncached store is not accepted until the prior B response is observed.
No architectural pipeline stage or extra benchmark cycle is introduced.

## Integration and rollback

Each boundary has a separate elaboration-time switch in
`MemoryControlConfig`. `WeBattleProfiles.N13` enables the complete cluster;
N12 remains an exact fallback. Development ablations can enable one boundary
at a time without creating release checkpoints.

N13 promotion gates are:

- Scala/Spinal elaboration for simulation and synthesis profiles;
- official 58/58 functional tests;
- official 20/20 performance tests and per-test N12/N10 comparison;
- routed WNS >= 0 at 10 ns, zero route errors and full resource reporting.

All gates passed. N13 is the accepted ownership head at **40.91%**. All 20
official CPU and SoC cycle counts are exactly equal to N12, while routed WNS
improves from +0.010 ns to +0.020 ns at the 10 ns constraint. N10 remains the
strict performance head because N13's combined route-derived score is 0.168%
lower.

## Validation and ablation results

- official functionality: **58/58 PASS**;
- official performance: **20/20 PASS**;
- CPU/SoC geometric means: 172,821.02 / 194,719.81 cycles;
- routed WNS/TNS/failing endpoints: +0.020 ns / 0 / 0;
- route status: zero routing errors;
- routed resources: 44,729 LUT, 20,501 FF, 30.5 BRAM and 4 DSP;
- route-derived Fmax: 100.200 MHz.

Synthesis-only ablations were used to reject misleading local conclusions.
The inherited store buffer improved synthesis WNS but increased LUT use by
3,548 compared with the complete N13 cluster, so the independent store-buffer
implementation was retained. Reverting address generation or load
postprocessing also increased LUT use. Reverting execute or uncached control
produced the same synthesis summary as the complete cluster, which indicates
that these boundaries are not the current timing or area limit. The full
routed result, rather than any synthesis-only ablation, is the acceptance
criterion.
