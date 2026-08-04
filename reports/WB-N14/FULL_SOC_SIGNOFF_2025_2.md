# WB-N14 full-SoC emergency sign-off

Date: 2026-08-04

Tool: Vivado 2025.2

Device/project: official chiplab NSCSCC team SoC project, xc7a200t

## Result

- Synthesis: completed, 0 errors.
- Implementation and route: completed; 71,487/71,487 routable nets fully routed; routing errors 0.
- Bitstream: generated successfully; pre-bitstream DRC 0 errors.
- CPU clock: 32.727 MHz (30.556 ns period).
- Final setup: WNS +0.978 ns, TNS 0, failing endpoints 0.
- Final hold: WHS +0.052 ns, THS 0, failing endpoints 0.
- Pulse-width: WPWS 0.000 ns, TPWS 0, failing endpoints 0.
- Bus skew: 12/12 reported constraints met.

## CDC constraint disclosure

The first routed report from Vivado 2025.2 had one hold path at -0.042 ns. The
path is inside the official Axi_CDC Gray-pointer synchronizer and crosses from
`sys_clk` to MIG UI clock `clk_pll_i`. The official `soc_lite.xdc` already
declares the major clocks asynchronous but does not include `clk_pll_i` in that
group. The sign-off checkpoint therefore adds only this clock-domain relation:

```tcl
set_clock_groups -asynchronous \
  -group [get_clocks sys_clk] \
  -group [get_clocks clk_pll_i]
```

No CPU path and no synchronous same-clock path is relaxed. The original routed
report and exact Tcl constraint remain in the local evidence archive.

## Boundary

This emergency build uses Vivado 2025.2. It does not replace the required
Vivado 2023.2 rebuild and target-board tests. The generated bitstream is a
candidate artifact, not proof of board stability or official score.
