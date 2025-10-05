Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ai = 'C:\Users\Diganta1\AndroidStudioProjects\AINoteBuddy\app\src\main\java\com\ainotebuddy\app\search\AIEnhancedSearchEngine.kt'
if (-not (Test-Path -LiteralPath $ai)) { Write-Error "AIEnhancedSearchEngine.kt not found at $ai"; exit 1 }

$lines = Get-Content -LiteralPath $ai
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match 'return@withContext\s+cachedResult') {
        if ($i + 2 -lt $lines.Count -and $lines[$i+1].Trim() -eq '}' -and $lines[$i+2].Trim() -eq '}') {
            $before = $lines[0..($i+1)]
            $after = @()
            if ($i + 3 -le $lines.Count - 1) { $after = $lines[($i+3)..($lines.Count-1)] }
            $lines = $before + $after
            break
        }
    }
}
Set-Content -LiteralPath $ai -Value $lines -Encoding UTF8
Write-Host 'Fixed extra closing brace in AIEnhancedSearchEngine.kt'
