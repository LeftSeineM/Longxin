param(
    [Parameter(Mandatory = $true)]
    [string]$BaselineCsv,
    [Parameter(Mandatory = $true)]
    [string]$CandidateCsv,
    [Parameter(Mandatory = $true)]
    [string]$OutputMarkdown,
    [Parameter(Mandatory = $true)]
    [string]$OutputCsv,
    [string]$BaselineName = 'Baseline',
    [string]$CandidateName = 'Candidate'
)

$ErrorActionPreference = 'Stop'
$baseline = @{}
Import-Csv -LiteralPath $BaselineCsv | ForEach-Object { $baseline[$_.Test] = $_ }
$candidate = @{}
Import-Csv -LiteralPath $CandidateCsv | ForEach-Object { $candidate[$_.Test] = $_ }

$order = @(
    'bitcount', 'bubble_sort', 'coremark', 'crc32', 'dhrystone',
    'quick_sort', 'select_sort', 'sha', 'stream_copy', 'stringsearch',
    'fireye_A0', 'fireye_B2', 'fireye_C0', 'fireye_D1', 'fireye_I2',
    'inner_product', 'lookup_table', 'loop_induction', 'my_memcmp',
    'minmax_sequence'
)

$rows = foreach ($test in $order) {
    if (-not $baseline.ContainsKey($test) -or -not $candidate.ContainsKey($test)) {
        throw "Missing baseline or candidate result for $test"
    }
    $b = $baseline[$test]
    $c = $candidate[$test]
    $bCpu = [double]$b.CPUCount
    $cCpu = [double]$c.CPUCount
    $bSoc = [double]$b.SoCCount
    $cSoc = [double]$c.SoCCount
    $bMp = [double]$b.BranchMispred
    $cMp = [double]$c.BranchMispred
    [pscustomobject]@{
        Test = $test
        BaselineCPU = [long]$bCpu
        CandidateCPU = [long]$cCpu
        CPUCycleReductionPct = 100.0 * ($bCpu - $cCpu) / $bCpu
        BaselineSoC = [long]$bSoc
        CandidateSoC = [long]$cSoc
        SoCCycleReductionPct = 100.0 * ($bSoc - $cSoc) / $bSoc
        BaselineBranchMispred = [long]$bMp
        CandidateBranchMispred = [long]$cMp
        BranchMispredReductionPct =
            if ($bMp -ne 0) { 100.0 * ($bMp - $cMp) / $bMp } else { 0.0 }
    }
}

$outputDir = Split-Path -Parent $OutputMarkdown
if ($outputDir) {
    New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
}
$rows | Export-Csv -NoTypeInformation -Encoding UTF8 -LiteralPath $OutputCsv

function Get-Geomean([double[]]$Values) {
    [math]::Exp(($Values | ForEach-Object { [math]::Log($_) } |
        Measure-Object -Average).Average)
}

$bCpuGeo = Get-Geomean @($rows.BaselineCPU)
$cCpuGeo = Get-Geomean @($rows.CandidateCPU)
$bSocGeo = Get-Geomean @($rows.BaselineSoC)
$cSocGeo = Get-Geomean @($rows.CandidateSoC)

$lines = [Collections.Generic.List[string]]::new()
$lines.Add("# $CandidateName versus $BaselineName performance")
$lines.Add('')
$lines.Add("- CPU-cycle geometric mean: $('{0:N2}' -f $bCpuGeo) -> $('{0:N2}' -f $cCpuGeo)")
$lines.Add("- CPU-cycle speedup: $('{0:F6}' -f ($bCpuGeo / $cCpuGeo))x ($('{0:F3}' -f (100.0 * ($bCpuGeo / $cCpuGeo - 1.0)))%)")
$lines.Add("- SoC-cycle geometric mean: $('{0:N2}' -f $bSocGeo) -> $('{0:N2}' -f $cSocGeo)")
$lines.Add("- SoC-cycle speedup: $('{0:F6}' -f ($bSocGeo / $cSocGeo))x ($('{0:F3}' -f (100.0 * ($bSocGeo / $cSocGeo - 1.0)))%)")
$lines.Add('')
$lines.Add("| Test | Baseline CPU | Candidate CPU | CPU cycles reduced | Branch MP baseline | Branch MP candidate | MP reduced |")
$lines.Add('|---|---:|---:|---:|---:|---:|---:|')
foreach ($row in $rows) {
    $lines.Add(
        "| $($row.Test) | $($row.BaselineCPU) | $($row.CandidateCPU) | " +
        "$('{0:F3}%' -f $row.CPUCycleReductionPct) | " +
        "$($row.BaselineBranchMispred) | $($row.CandidateBranchMispred) | " +
        "$('{0:F3}%' -f $row.BranchMispredReductionPct) |"
    )
}
[IO.File]::WriteAllLines($OutputMarkdown, $lines, [Text.UTF8Encoding]::new($false))

Write-Host "Comparison: $CandidateName versus $BaselineName"
Write-Host "CPU geomean speedup: $('{0:F6}' -f ($bCpuGeo / $cCpuGeo))x"
Write-Host "SoC geomean speedup: $('{0:F6}' -f ($bSocGeo / $cSocGeo))x"
Write-Host "Markdown: $OutputMarkdown"
Write-Host "CSV: $OutputCsv"
