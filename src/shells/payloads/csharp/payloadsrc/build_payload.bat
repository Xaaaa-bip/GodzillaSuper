@echo off
rem Build the C# payload DLL. Framework 2.0 csc on purpose: the assembly has to
rem load on old CLR hosts, so no var / lambda / auto-property / LINQ.
rem Every .cs in this directory must be listed, or it silently does not ship.
set FX=C:\Windows\Microsoft.NET\Framework\v2.0.50727
"%FX%\csc.exe" /nologo /noconfig /target:library /filealign:512 /optimize+ /out:"%~dp0..\assets\payload.dll" /reference:"%FX%\System.dll" /reference:"%FX%\System.Data.dll" "%~dp0LY.cs" "%~dp0NxTop.cs" "%~dp0NxJob.cs" "%~dp0Native.cs" "%~dp0Amsi.cs" "%~dp0Etw.cs" "%~dp0Hwbp.cs"
