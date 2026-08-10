param(
    [Parameter(Mandatory = $true)]
    [ValidateSet(
        "win-x64-ex",
        "macosx-x64-ex",
        "macosx-aarch64-ex",
        "linux-x64-ex",
        "linux-aarch64-ex"
    )]
    [string]$Target
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$buildRoot = Join-Path $repoRoot "build"
$distributionRoot = Join-Path $buildRoot "distributions"
$packageRoot = Join-Path $distributionRoot $Target
$libDest = Join-Path $packageRoot "logbook_lib"
$zipDest = Join-Path $distributionRoot "logbook.$Target.zip"
$runtimeSource = Join-Path $buildRoot "runtime"
$runtimeLibSource = Join-Path $buildRoot "runtime-libs"
$jarSource = Join-Path (Join-Path $buildRoot "libs") "logbook.jar"

foreach ($requiredPath in @($runtimeSource, $runtimeLibSource, $jarSource)) {
    if (-not (Test-Path -LiteralPath $requiredPath)) {
        throw "Required build input does not exist: $requiredPath"
    }
}
if (-not (Test-Path -LiteralPath (Join-Path $runtimeLibSource "swt.jar"))) {
    throw "The target SWT library does not exist in $runtimeLibSource"
}

if (Test-Path -LiteralPath $packageRoot) {
    Remove-Item -LiteralPath $packageRoot -Recurse -Force
}
if (Test-Path -LiteralPath $zipDest) {
    Remove-Item -LiteralPath $zipDest -Force
}

New-Item -Path $libDest -ItemType Directory -Force | Out-Null
Copy-Item (Join-Path $runtimeLibSource "*") $libDest -Recurse
Copy-Item -LiteralPath $jarSource -Destination $packageRoot
Copy-Item -LiteralPath $runtimeSource -Destination (Join-Path $packageRoot "runtime") -Recurse

if ($Target -eq "win-x64-ex") {
    $launcher = Join-Path (Join-Path (Join-Path $repoRoot "launcher") "win-x64-ex") "logbook.exe"
    if (-not (Test-Path -LiteralPath $launcher)) {
        throw "Windows launcher does not exist: $launcher"
    }
    Copy-Item -LiteralPath $launcher -Destination $packageRoot
}
else {
    $launcherName = if ($Target.StartsWith("macosx-")) { "logbook_macos.sh" } else { "logbook_linux.sh" }
    $launcherSource = Join-Path (Join-Path $repoRoot "launcher") $launcherName
    $launcherDest = Join-Path $packageRoot "logbook"
    Copy-Item -LiteralPath $launcherSource -Destination $launcherDest
    & chmod +x $launcherDest
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to make the launcher executable"
    }
}

if ($IsWindows) {
    Compress-Archive -Path $packageRoot -DestinationPath $zipDest
}
else {
    Push-Location $distributionRoot
    try {
        & zip -q -r $zipDest $Target
        if ($LASTEXITCODE -ne 0) {
            throw "zip failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

Write-Host "Created $zipDest"
