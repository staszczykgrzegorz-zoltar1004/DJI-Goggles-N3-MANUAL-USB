param([string]$JavaHome = $env:JAVA_HOME)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome) {
    $localTools = if ($env:N3_ANDROID_TOOLS) { $env:N3_ANDROID_TOOLS } else { Join-Path $env:USERPROFILE 'Documents\apk pogoda\.tools' }
    $JavaHome = Join-Path $localTools 'jdk-17'
}
$javaBin = Join-Path $JavaHome 'bin'
$source = Join-Path $PSScriptRoot 'app\src\main\java\pl\stasz\n3manual'
$destination = Join-Path $PSScriptRoot 'test-build'
New-Item -ItemType Directory -Force -Path $destination | Out-Null
& (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -d $destination (Join-Path $source 'Duml.java') (Join-Path $source 'LogicTunnel.java') (Join-Path $source 'ManualWizard.java') (Join-Path $PSScriptRoot 'tests\CoreTests.java')
if ($LASTEXITCODE -ne 0) { throw 'javac test compile failed' }
& (Join-Path $javaBin 'java.exe') -cp $destination pl.stasz.n3manual.CoreTests | Tee-Object -FilePath (Join-Path $destination 'results.txt')
if ($LASTEXITCODE -ne 0) { throw 'Java tests failed' }
