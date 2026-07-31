# WB-N14 instruction-front-end replacement

Status: **accepted ownership checkpoint; 58/58, 20/20 and routed timing pass**.

Baseline: `WB-N13-20PASS`.

## Goal and frozen weight

N14 replaces two coherent front-end boundaries in one checkpoint:

1. I-cache hit selection, refill sequencing, packet assembly and invalidate
   control (frozen upstream weight: 253 lines);
2. BTB lookup/update, global+tournament direction prediction, speculative GHR
   movement and commit recovery (frozen upstream weight: 215 lines).

The RAM primitives remain standard reusable infrastructure. The new
controllers own the request, state-transition, arbitration and update
contracts around those memories.

If both boundaries pass every gate, accepted component coverage becomes:

`(1,960 + 253 + 215) / 4,791 = 50.68%`

## I-cache contract

- IF1 reads the virtually indexed tag/data arrays using the next fetch PC.
- IF2 compares physical tags and emits up to four words without crossing a
  cache-line boundary.
- A miss launches exactly one aligned AXI incrementing burst and blocks IF2
  until the full line is present.
- Refill writes one word per response beat, captures requested words for the
  waiting packet, updates tag/LRU/valid atomically, then refetches IF1.
- Index invalidate, hit invalidate and full invalidate preserve the privileged
  cache-operation semantics of N13.
- The intended hit path and miss penalty remain cycle-compatible with N13.

## BTB and predictor contract

- Four adjacent BTB entries and predictor counters are read per fetch group.
- The earliest valid predicted-taken instruction wins and masks younger words.
- Global history moves speculatively at IF2 firing and is restored atomically
  on a committed misprediction.
- The global table uses the selected gselect/bank-safe-gshare index policy.
- The packed tournament table holds bimodal and chooser counters in one word;
  chooser movement occurs only when global and bimodal predictions disagree.
- Commit may invalidate a stale branch, allocate a new branch or update an
  existing branch with saturating counters and current target metadata.
- RAS target override remains a separate accepted N7 boundary.

## Isolation and promotion gates

`FrontendConfig` exposes separate I-cache and predictor/BTB switches. N14
enables both; `N14NOICACHE` and `N14NOPRED` are diagnostic fallbacks. N13 is
the immutable rollback package.

N14 receives ownership credit only after:

- simulation and synthesis RTL elaborate successfully;
- official functionality reaches 58/58;
- official performance reaches 20/20 with per-test N13/N10 comparison;
- routed WNS is non-negative at 10 ns with zero routing errors;
- LUT, FF, BRAM, DSP and raw audit differences are recorded.

All gates passed. N14 is the accepted ownership head at **50.68%**. Every
official CPU count, SoC count, cache-miss counter and branch-misprediction
counter is exactly equal to N13. Post-route AggressiveExplore closes timing at
WNS +0.006 ns, TNS 0 and zero failing endpoints/routing errors.

The routed design uses 45,309 LUTs, 20,518 flip-flops, 30.5 BRAM and 4 DSP.
Its route-derived Fmax is 100.060 MHz. Because N13 routed at 100.200 MHz with
the same cycle counts, N14's combined estimate is 0.140% lower. N10 remains
the strict performance head; N14 is the ownership/submission-integration head.
