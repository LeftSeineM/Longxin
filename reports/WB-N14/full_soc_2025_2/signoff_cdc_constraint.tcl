open_checkpoint {D:/NSCSCC2026/chiplab-wb-n14-build/fpga/nscscc-team/run_vivado/project/loongson.runs/impl_1/soc_top_routed.dcp}

# The official SoC instantiates Axi_CDC between sys_clk and the MIG UI clock
# clk_pll_i.  soc_lite.xdc groups clk/sys/cpu/ddr but omits clk_pll_i, causing
# Vivado 2025.2 to time a Gray-pointer synchronizer as a synchronous hold path.
# Classify the two clock domains as asynchronous; the RTL synchronizer remains
# intact and is still covered by CDC structure review.
set_clock_groups -asynchronous \
  -group [get_clocks sys_clk] \
  -group [get_clocks clk_pll_i]

report_timing_summary -delay_type min_max -max_paths 20 -report_unconstrained \
  -file {D:/NSCSCC2026/chiplab-wb-n14-build/fpga/nscscc-team/run_vivado/project/loongson.runs/impl_1/soc_top_timing_summary_cdc_signoff.rpt}
report_route_status \
  -file {D:/NSCSCC2026/chiplab-wb-n14-build/fpga/nscscc-team/run_vivado/project/loongson.runs/impl_1/soc_top_route_status_cdc_signoff.rpt}
report_bus_skew -warn_on_violation \
  -file {D:/NSCSCC2026/chiplab-wb-n14-build/fpga/nscscc-team/run_vivado/project/loongson.runs/impl_1/soc_top_bus_skew_cdc_signoff.rpt}
write_checkpoint -force \
  {D:/NSCSCC2026/chiplab-wb-n14-build/fpga/nscscc-team/run_vivado/project/loongson.runs/impl_1/soc_top_cdc_signoff.dcp}
write_bitstream -force \
  {D:/NSCSCC2026/chiplab-wb-n14-build/fpga/nscscc-team/run_vivado/project/loongson.runs/impl_1/soc_top_cdc_signoff.bit}
exit
