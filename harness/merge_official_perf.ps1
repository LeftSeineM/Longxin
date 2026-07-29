param(
    [Parameter(Mandatory = $true)]
    [string[]]$LogPath,
    [Parameter(Mandatory = $true)]
    [string]$OutputMarkdown,
    [Parameter(Mandatory = $true)]
    [string]$OutputCsv,
    [string]$Title = 'Official 20-test performance result'
)

$ErrorActionPreference = 'Stop'
$order = @(
    'bitcount', 'bubble_sort', 'coremark', 'crc32', 'dhrystone',
    'quick_sort', 'select_sort', 'sha', 'stream_copy', 'stringsearch',
    'fireye_A0', 'fireye_B2', 'fireye_C0', 'fireye_D1', 'fireye_I2',
    'inner_product', 'lookup_table', 'loop_induction', 'my_memcmp',
    'minmax_sequence'
)

$results = @{}
foreach ($path in $LogPath) {
    $resolved = (Resolve-Path -LiteralPath $path).Path
    $text = [IO.File]::ReadAllText($resolved)
    $sections = [regex]::Matches(
        $text,
        '(?ms)^PulseLA official performance test: (?<name>[^\r\n]+)\r?\n.*?(?=^PulseLA official performance test: |\z)'
    )
    foreach ($sectionMatch in $sections) {
        $name = $sectionMatch.Groups['name'].Value.Trim()
        if ($order -notcontains $name) {
            continue
        }
        $section = $sectionMatch.Value
        if ($section -notmatch 'PASS!') {
            continue
        }
        $socMatch = [regex]::Match($section, 'Total Count\(SoC count\)\s*=\s*0x([0-9a-fA-F]+)')
        $cpuMatch = [regex]::Match($section, 'Total Count\(CPU count\)\s*=\s*0x([0-9a-fA-F]+)')
        if (-not $socMatch.Success -or -not $cpuMatch.Success) {
            continue
        }

        $counters = @{}
        foreach ($counterMatch in [regex]::Matches(
            $section,
            '(?m)^PULSE_COUNTER\s+([a-z_]+)=(\d+)\s*$'
        )) {
            $counters[$counterMatch.Groups[1].Value] = [long]$counterMatch.Groups[2].Value
        }

        $soc = [Convert]::ToInt64($socMatch.Groups[1].Value, 16)
        $cpu = [Convert]::ToInt64($cpuMatch.Groups[1].Value, 16)
        $commit = if ($counters.ContainsKey('commit_inst')) { $counters['commit_inst'] } else { 0 }
        $branch = if ($counters.ContainsKey('br_inst')) { $counters['br_inst'] } else { 0 }
        $results[$name] = [pscustomobject]@{
            Test = $name
            Status = 'PASS'
            SoCCount = $soc
            CPUCount = $cpu
            Commit = $commit
            CommitPerScoreCycle = if ($cpu -ne 0 -and $commit -ne 0) { $commit / $cpu } else { 0 }
            ICacheMiss = if ($counters.ContainsKey('icache_miss')) { $counters['icache_miss'] } else { 0 }
            DCacheMiss = if ($counters.ContainsKey('dcache_miss')) { $counters['dcache_miss'] } else { 0 }
            MemoryInst = if ($counters.ContainsKey('mem_inst')) { $counters['mem_inst'] } else { 0 }
            BranchInst = $branch
            BranchPred = if ($counters.ContainsKey('br_pre')) { $counters['br_pre'] } else { 0 }
            BranchMispred = if ($counters.ContainsKey('br_pre_error')) { $counters['br_pre_error'] } else { 0 }
            MispredRate = if ($branch -ne 0) { $counters['br_pre_error'] / $branch } else { 0 }
        }
    }
}

$rows = @($order | Where-Object { $results.ContainsKey($_) } | ForEach-Object { $results[$_] })
$outputDir = Split-Path -Parent $OutputMarkdown
if ($outputDir) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -Path $OutputCsv

$socGeo = [math]::Exp(($rows | ForEach-Object { [math]::Log($_.SoCCount) } | Measure-Object -Average).Average)
$cpuGeo = [math]::Exp(($rows | ForEach-Object { [math]::Log($_.CPUCount) } | Measure-Object -Average).Average)
$lines = [Collections.Generic.List[string]]::new()
$lines.Add("# $Title")
$lines.Add('')
$lines.Add("- Result: $($rows.Count) / 20 PASS")
$lines.Add("- SoC-count geometric mean: $([math]::Round($socGeo, 2))")
$lines.Add("- CPU-count geometric mean: $([math]::Round($cpuGeo, 2))")
$lines.Add("- Logs: $($LogPath -join '; ')")
$lines.Add('- Note: performance counters include boot/measurement regions that differ from official CPU Count; Commit/CPU is diagnostic, not architectural IPC.')
$lines.Add('')
$lines.Add('| Test | SoC cycles | CPU cycles | Commit | Commit/CPU* | I$ miss | D$ miss | Branch MP | MP rate |')
$lines.Add('|---|---:|---:|---:|---:|---:|---:|---:|---:|')
foreach ($row in $rows) {
    $lines.Add(
        "| $($row.Test) | $($row.SoCCount) | $($row.CPUCount) | $($row.Commit) | " +
        "$('{0:F4}' -f $row.CommitPerScoreCycle) | $($row.ICacheMiss) | $($row.DCacheMiss) | " +
        "$($row.BranchMispred) | $('{0:P2}' -f $row.MispredRate) |"
    )
}
[IO.File]::WriteAllLines($OutputMarkdown, $lines, [Text.UTF8Encoding]::new($false))

Write-Host "Merged official performance result: $($rows.Count)/20 PASS"
Write-Host "Markdown: $OutputMarkdown"
Write-Host "CSV: $OutputCsv"
if ($rows.Count -ne 20) {
    throw "Expected 20 completed benchmarks, found $($rows.Count)"
}
