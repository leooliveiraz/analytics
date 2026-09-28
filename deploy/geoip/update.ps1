param(
    [string]$Distro = "Ubuntu-24.04"
)

$ErrorActionPreference = "Stop"

$here = $PSScriptRoot
if ($here -match '^([A-Za-z]):\\(.*)$') {
    $wslPath = "/mnt/" + $Matches[1].ToLower() + "/" + $Matches[2].Replace('\', '/')
}
else {
    throw "Caminho inesperado: $here"
}

Write-Host "Executando update do GeoLite2 em $wslPath ..." -ForegroundColor Cyan
wsl -d $Distro -- bash -lc "bash '$wslPath/update.sh'"
