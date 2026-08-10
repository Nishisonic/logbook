Copy-Item ..\build\libs\logbook.jar logbook.jar -Force

# SWTがwin32.win32.x86/gtk.linux.x86の提供を3.108.0で終了しており、JDK 25自体も32bit版が
# 存在しないためx86(32bit)は対象から除外
$targets = @("win-x64-ex", "macosx-x64-ex", "macosx-aarch64-ex", "linux-x64-ex", "linux-aarch64-ex")

foreach ($t in $targets) {
    $dest = ".\$t"
    $libDest = Join-Path $dest "logbook_lib"

    if (Test-Path $dest) {
        Remove-Item $dest -Recurse
    }
    if (Test-Path ".\logbook.$t.zip") {
        Remove-Item ".\logbook.$t.zip"
    }

    New-Item $dest -ItemType Directory -Force
    New-Item $libDest -ItemType Directory -Force

    Copy-Item "..\lib\*" $libDest -Recurse
    Copy-Item ".\logbook.jar" $dest
    Copy-Item ".\swt\$t\swt.jar" (Join-Path $libDest "swt.jar") -Force

    Compress-Archive -Path $dest -DestinationPath ".\logbook.$t.zip"

    Remove-Item $dest -Recurse
}
