# Builds build/adb-wifi-toggle.apk with the plain Android SDK tools (no Gradle).
# Requires: Android SDK (build-tools, platform android-36) and JDK 17+.
param(
    [string]$Sdk = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }),
    [string]$BuildTools = "36.1.0",
    [string]$Platform = "android-36",
    [string]$Keystore = "$env:USERPROFILE\.android\debug.keystore"
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$bt = Join-Path $Sdk "build-tools\$BuildTools"
$androidJar = Join-Path $Sdk "platforms\$Platform\android.jar"
$out = Join-Path $PSScriptRoot 'build'

function Invoke-Tool { param([string]$Exe) & $Exe @args; if ($LASTEXITCODE) { throw "$Exe failed ($LASTEXITCODE)" } }

Remove-Item $out -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force "$out\res", "$out\gen", "$out\classes", "$out\dex" | Out-Null

Invoke-Tool "$bt\aapt2.exe" compile --dir res -o "$out\res\res.zip"
Invoke-Tool "$bt\aapt2.exe" link -o "$out\unsigned.apk" -I $androidJar --manifest AndroidManifest.xml `
    --java "$out\gen" "$out\res\res.zip" --auto-add-overlay

$sources = @(Get-ChildItem src, "$out\gen" -Recurse -Filter *.java | ForEach-Object FullName)
Invoke-Tool javac --release 17 -encoding UTF-8 -classpath $androidJar -d "$out\classes" @sources

$classes = @(Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object FullName)
Invoke-Tool "$bt\d8.bat" --release --min-api 30 --lib $androidJar --output "$out\dex" @classes

# add classes.dex to the apk root (aapt keeps resources.arsc uncompressed, jar would not)
Push-Location "$out\dex"
try { Invoke-Tool "$bt\aapt.exe" add "$out\unsigned.apk" classes.dex } finally { Pop-Location }

Invoke-Tool "$bt\zipalign.exe" -f -p 4 "$out\unsigned.apk" "$out\aligned.apk"
Invoke-Tool "$bt\apksigner.bat" sign --ks $Keystore --ks-pass pass:android --key-pass pass:android `
    --out "$out\adb-wifi-toggle.apk" "$out\aligned.apk"
Write-Host "Built $out\adb-wifi-toggle.apk"
