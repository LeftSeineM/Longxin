if {$argc != 2} {
    error "usage: post_route_physopt.tcl <routed-checkpoint> <report-root>"
}

set checkpoint [file normalize [lindex $argv 0]]
set report_root [file normalize [lindex $argv 1]]

if {![file exists $checkpoint]} {
    error "routed checkpoint does not exist: $checkpoint"
}
file mkdir $report_root

open_checkpoint $checkpoint
phys_opt_design -directive AggressiveExplore
route_design

report_timing_summary -delay_type max -max_paths 20 \
    -file [file join $report_root timing_route_post_physopt_10ns.rpt]
report_utilization -file [file join $report_root utilization_route_post_physopt.rpt]
report_route_status -file [file join $report_root route_status_post_physopt.rpt]
write_checkpoint -force [file join $report_root post_route_physopt.dcp]

close_project
