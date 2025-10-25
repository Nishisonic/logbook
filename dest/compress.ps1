# initialize
Remove-Item .\win-x64-ex -Recurse
Remove-Item .\win-x86-ex -Recurse
Remove-Item .\macosx-x64-ex -Recurse
Remove-Item .\linux-x64-ex -Recurse
Remove-Item .\linux-x86-ex -Recurse
# initialize
Remove-Item .\logbook.win-x64-ex.zip
Remove-Item .\logbook.win-x86-ex.zip
Remove-Item .\logbook.macosx-x64-ex.zip
Remove-Item .\logbook.linux-x64-ex.zip
Remove-Item .\logbook.linux-x86-ex.zip
# make tmp dir
New-Item .\win-x64-ex -ItemType Directory
New-Item .\win-x86-ex -ItemType Directory
New-Item .\macosx-x64-ex -ItemType Directory
New-Item .\linux-x64-ex -ItemType Directory
New-Item .\linux-x86-ex -ItemType Directory
# copy
Copy-Item -Path .\logbook_lib,.\logbook.jar -Destination .\win-x64-ex -Recurse
Copy-Item -Path .\swt\win-x64-ex\swt.jar -Destination .\win-x64-ex\logbook_lib\swt.jar -Force
Copy-Item -Path .\logbook_lib,.\logbook.jar -Destination .\win-x86-ex -Recurse
Copy-Item -Path .\swt\win-x86-ex\swt.jar -Destination .\win-x86-ex\logbook_lib\swt.jar -Force
Copy-Item -Path .\logbook_lib,.\logbook.jar -Destination .\macosx-x64-ex -Recurse
Copy-Item -Path .\swt\macosx-x64-ex\swt.jar -Destination .\macosx-x64-ex\logbook_lib\swt.jar -Force
Copy-Item -Path .\logbook_lib,.\logbook.jar -Destination .\linux-x64-ex -Recurse
Copy-Item -Path .\swt\linux-x64-ex\swt.jar -Destination .\linux-x64-ex\logbook_lib\swt.jar -Force
Copy-Item -Path .\logbook_lib,.\logbook.jar -Destination .\linux-x86-ex -Recurse
Copy-Item -Path .\swt\linux-x86-ex\swt.jar -Destination .\linux-x86-ex\logbook_lib\swt.jar -Force
# compress
Compress-Archive -Path .\win-x64-ex -DestinationPath .\logbook.win-x64-ex.zip
Compress-Archive -Path .\win-x86-ex -DestinationPath .\logbook.win-x86-ex.zip
Compress-Archive -Path .\macosx-x64-ex -DestinationPath .\logbook.macosx-x64-ex.zip
Compress-Archive -Path .\linux-x64-ex -DestinationPath .\logbook.linux-x64-ex.zip
Compress-Archive -Path .\linux-x86-ex -DestinationPath .\logbook.linux-x86-ex.zip
# remove tmp
Remove-Item .\win-x64-ex -Recurse
Remove-Item .\win-x86-ex -Recurse
Remove-Item .\macosx-x64-ex -Recurse
Remove-Item .\linux-x64-ex -Recurse
Remove-Item .\linux-x86-ex -Recurse
