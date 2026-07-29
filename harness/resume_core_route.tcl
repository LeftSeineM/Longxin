if {$argc != 2} {
    error "usage: resume_core_route.tcl <post-synth-checkpoint> <report-root>"
}

set checkpoint [file normalize [lindex $argv 0]]
set report_root [file normalize [lindex $argv 1]]

if {![file exists $checkpoint]} {
    error "post-synthesis checkpoint does not exist: $checkpoint"
}
file mkdir $report_root

open_checkpoint $checkpoint
opt_design
place_design
phys_opt_design
route_design

report_timing_summary -delay_type max -max_paths 20 \
    -file [file join $report_root timing_route_10ns.rpt]
report_utilization -file [file join $report_root utilization_route.rpt]
report_route_status -file [file join $report_root route_status.rpt]
write_checkpoint -force [file join $report_root post_route.dcp]

close_project
