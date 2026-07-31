if {$argc != 2} {
    error "usage: run_core_synthesis.tcl <generated-core-root> <report-root>"
}

set core_root [file normalize [lindex $argv 0]]
set report_root [file normalize [lindex $argv 1]]
set part_name xc7a200tfbg676-2

file mkdir $report_root

set wrapper [file join $core_root core_top.sv]
set generated [file join $core_root wb_raw_top.v]
set multiplier [file join $core_root xilinx_ip multiplier.xci]

foreach required [list $wrapper $generated $multiplier] {
    if {![file exists $required]} {
        error "required synthesis input does not exist: $required"
    }
}

create_project -in_memory -part $part_name
read_verilog -sv $wrapper
read_verilog $generated
read_ip $multiplier

synth_design -top core_top -part $part_name -flatten_hierarchy rebuilt
create_clock -name aclk -period 10.000 [get_ports aclk]
report_timing_summary -delay_type max -max_paths 20 \
    -file [file join $report_root timing_synth_10ns.rpt]
report_utilization -file [file join $report_root utilization_synth.rpt]
write_checkpoint -force [file join $report_root post_synth.dcp]

close_project
