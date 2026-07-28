if {$argc != 2} {
    error "usage: report_hier.tcl <post-synth-dcp> <output-report>"
}
open_checkpoint [file normalize [lindex $argv 0]]
report_utilization -hierarchical -hierarchical_depth 4 \
    -file [file normalize [lindex $argv 1]]
close_design
