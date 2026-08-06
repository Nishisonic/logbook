Copy-Item ..\build\libs\logbook.jar logbook.jar -Force

$targets = @("win-x64-ex", "win-x86-ex", "macosx-x64-ex", "macosx-aarch64-ex", "linux-x64-ex", "linux-x86-ex", "linux-aarch64-ex")

foreach ($t in $targets) {
    $dest = ".\$t"
    $libDest = Join-Path $dest "logbook_lib"

    Remove-Item $dest -Recurse
    Remove-Item ".\logbook.$t.zip"
    
    New-Item $dest -ItemType Directory -Force
    New-Item $libDest -ItemType Directory -Force

    Copy-Item "..\lib\*" $libDest -Recurse
    Copy-Item ".\logbook.jar" $dest
    Copy-Item ".\swt\$t\swt.jar" (Join-Path $libDest "swt.jar") -Force

    Compress-Archive -Path $dest -DestinationPath ".\logbook.$t.zip"

    Remove-Item $dest -Recurse
}
