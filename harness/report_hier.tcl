if {$argc < 2 || $argc > 3} {
    error "usage: report_hier.tcl <post-synth-dcp> <output-report> ?hierarchical-depth?"
}
open_checkpoint [file normalize [lindex $argv 0]]
set hierarchy_depth [expr {$argc == 3 ? [lindex $argv 2] : 4}]
report_utilization -hierarchical -hierarchical_depth $hierarchy_depth \
    -file [file normalize [lindex $argv 1]]
close_design
