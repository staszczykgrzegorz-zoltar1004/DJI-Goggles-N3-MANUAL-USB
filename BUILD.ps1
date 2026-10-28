param([switch]$Offline, [switch]$Release, [string]$ToolsRoot = $env:N3_ANDROID_TOOLS)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
if (-not $ToolsRoot) { $ToolsRoot = Join-Path $env:USERPROFILE 'Documents\apk pogoda\.tools' }
$buildTools = $ToolsRoot
$env:JAVA_HOME = Join-Path $buildTools 'jdk-17'
$env:ANDROID_HOME = Join-Path $buildTools 'android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$($env:JAVA_HOME)\bin;$env:Path"
Push-Location -LiteralPath $projectRoot
try {
    $variant = if ($Release) { 'Release' } else { 'Debug' }
    $gradleOptions = @('--no-daemon', ":app:assemble$variant", ":app:lint$variant")
    if ($Offline) { $gradleOptions += '--offline' }
    & (Join-Path $buildTools 'gradle-8.7\bin\gradle.bat') @gradleOptions
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed' }
    New-Item -ItemType Directory -Force -Path (Join-Path $projectRoot 'release') | Out-Null
    if (-not $Release) {
        Copy-Item -LiteralPath (Join-Path $projectRoot 'app\build\outputs\apk\debug\app-debug.apk') -Destination (Join-Path $projectRoot 'release\N3Manual-development.apk') -Force
    }
} finally { Pop-Location }
