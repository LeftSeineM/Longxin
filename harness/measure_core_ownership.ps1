param(
    [string]$UpstreamRoot = "D:\NSCSCC2026\OpenSource-Cores\NOP-Core",
    [string]$CurrentRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$OutputPath = ""
)

$ErrorActionPreference = "Stop"

function Get-CodeLineCount([string]$Path) {
    $total = 0
    Get-ChildItem -LiteralPath $Path -Recurse -Filter "*.scala" |
        ForEach-Object {
            $total += @(
                Get-Content -LiteralPath $_.FullName |
                    Where-Object {
                        $_ -notmatch '^\s*(//|/\*|\*|\*/)?\s*$'
                    }
            ).Count
        }
    return $total
}

$domains = @(
    "pipeline\fetch",
    "pipeline\decode",
    "pipeline\core",
    "pipeline\exe",
    "pipeline\mem"
)

$domainRows = foreach ($domain in $domains) {
    $upstreamPath = Join-Path (Join-Path $UpstreamRoot "src") $domain
    [pscustomobject]@{
        Domain = $domain
        UpstreamCodeLines = Get-CodeLineCount $upstreamPath
    }
}
$denominator = ($domainRows | Measure-Object UpstreamCodeLines -Sum).Sum

$independentFiles = Get-ChildItem -LiteralPath (Join-Path $CurrentRoot "src") `
    -Recurse -Filter "WeBattle*.scala"
$independentCode = 0
foreach ($file in $independentFiles) {
    $independentCode += @(
        Get-Content -LiteralPath $file.FullName |
            Where-Object {
                $_ -notmatch '^\s*(//|/\*|\*|\*/)?\s*$'
            }
    ).Count
}

$upstreamSource = Join-Path $UpstreamRoot "src"
$currentSource = Join-Path $CurrentRoot "src"
$savedErrorAction = $ErrorActionPreference
$ErrorActionPreference = "SilentlyContinue"
$diff = & git diff --no-index --numstat -- $upstreamSource $currentSource 2>$null
$ErrorActionPreference = $savedErrorAction
$diffRows = @(
    $diff | ForEach-Object {
        if ($_ -match '^(\d+)\s+(\d+)\s+(.+)$') {
            [pscustomobject]@{
                Added = [int]$Matches[1]
                Deleted = [int]$Matches[2]
                File = $Matches[3]
            }
        }
    }
)
$added = ($diffRows | Measure-Object Added -Sum).Sum
$deleted = ($diffRows | Measure-Object Deleted -Sum).Sum

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("# WeBattle core ownership measurement")
$lines.Add("")
$lines.Add("Generated: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')")
$lines.Add("")
$lines.Add("## Frozen denominator")
$lines.Add("")
$lines.Add("| Domain | Upstream nonblank code lines |")
$lines.Add("|---|---:|")
foreach ($row in $domainRows) {
    $lines.Add("| $($row.Domain) | $($row.UpstreamCodeLines) |")
}
$lines.Add("| Total | **$denominator** |")
$lines.Add("")
$lines.Add("## Secondary audit metrics")
$lines.Add("")
$lines.Add("- Independent `WeBattle*.scala` files: $($independentFiles.Count)")
$lines.Add("- Independent nonblank code lines: $independentCode")
$lines.Add("- Upstream/current changed or added files: $($diffRows.Count)")
$lines.Add("- Text additions/deletions: +$added / -$deleted")
$lines.Add("- Text churn versus upstream 8,336-line full source denominator: " +
    ("{0:N2}%" -f (100.0 * ($added + $deleted) / 8336.0)))
$lines.Add("")
$lines.Add("These are audit metrics, not an originality or performance claim. " +
    "Component-weighted accepted coverage is maintained in the design reports.")

$text = $lines -join [Environment]::NewLine
if ($OutputPath) {
    $parent = Split-Path -Parent $OutputPath
    if ($parent) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
    }
    [System.IO.File]::WriteAllText($OutputPath, $text)
}
$text
