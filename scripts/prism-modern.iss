#ifndef SourceDir
  #define SourceDir "install"
#endif
#ifndef OutDir
  #define OutDir "dist"
#endif

[Setup]
AppName=PrismLauncher
AppVersion=1.0
AppPublisher=Dasko2
DefaultDirName={localappdata}\Programs\PrismLauncher
DefaultGroupName=PrismLauncher
PrivilegesRequired=lowest
OutputDir={#OutDir}
OutputBaseFilename=PrismLauncher-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\prismlauncher.exe
DisableProgramGroupPage=yes

[Files]
Source: "{#SourceDir}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{autoprograms}\PrismLauncher"; Filename: "{app}\prismlauncher.exe"
Name: "{autodesktop}\PrismLauncher"; Filename: "{app}\prismlauncher.exe"

[Run]
Filename: "{app}\prismlauncher.exe"; Description: "Lancer maintenant"; Flags: nowait postinstall skipifsilent
