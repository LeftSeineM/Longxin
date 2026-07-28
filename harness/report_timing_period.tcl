if {$argc != 3} {
    error "usage: report_timing_period.tcl <post-route-dcp> <period-ns> <output-report>"
}

set checkpoint [file normalize [lindex $argv 0]]
set period_ns [lindex $argv 1]
set output_report [file normalize [lindex $argv 2]]

open_checkpoint $checkpoint

set clocks [get_clocks -quiet]
if {[llength $clocks] == 0} {
    error "checkpoint contains no clock"
}

set clock_name [get_property NAME [lindex $clocks 0]]
set clock_port [get_ports -quiet aclk]
if {[llength $clock_port] == 0} {
    set clock_port [get_ports -quiet clk]
}
if {[llength $clock_port] == 0} {
    error "neither aclk nor clk exists"
}

create_clock -name $clock_name -period $period_ns $clock_port
report_timing_summary -delay_type max -max_paths 20 -file $output_report
close_design
