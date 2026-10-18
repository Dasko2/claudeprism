; Installeur unique : un seul fichier Setup.exe, sans droits administrateur.
#ifndef SourceDir
  #define SourceDir "install"
#endif
#ifndef OutDir
  #define OutDir "dist"
#endif

[Setup]
AppName=Prism Launcher Moderne
AppVersion=1.0
AppPublisher=Dasko2
DefaultDirName={localappdata}\Programs\PrismLauncherModern
DefaultGroupName=Prism Launcher Moderne
PrivilegesRequired=lowest
OutputDir={#OutDir}
OutputBaseFilename=PrismLauncher-Setup
Compression=lzma2
SolidCompression=yes
ArchitecturesInstallIn64BitMode=x64compatible
UninstallDisplayIcon={app}\prismlauncher.exe
DisableProgramGroupPage=yes

[Files]
; l'agent est caché : pas visible dans l'explorateur ni dans la liste des mods
Source: "{#SourceDir}\features\*"; DestDir: "{app}\features"; Flags: recursesubdirs ignoreversion; Attribs: hidden system
Source: "{#SourceDir}\*"; DestDir: "{app}"; Excludes: "features"; Flags: recursesubdirs createallsubdirs ignoreversion

[Icons]
Name: "{autoprograms}\Prism Launcher Moderne"; Filename: "{app}\prismlauncher.exe"
Name: "{autodesktop}\Prism Launcher Moderne"; Filename: "{app}\prismlauncher.exe"

[Run]
Filename: "{app}\prismlauncher.exe"; Description: "Lancer maintenant"; Flags: nowait postinstall skipifsilent
