$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$packageInput = Join-Path $projectRoot "target\package-input"
$dist = Join-Path $projectRoot "dist"
$appName = "SpacedRepetition"
$appJarName = "SpacedRepetition-1.0-SNAPSHOT.jar"
$appIcon = Join-Path $projectRoot "src\main\resources\Icon.ico"
$javaFxBase = "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-base\25.0.4\javafx-base-25.0.4-win.jar"
$javaFxGraphics = "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-graphics\25.0.4\javafx-graphics-25.0.4-win.jar"
$javaFxControls = "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-controls\25.0.4\javafx-controls-25.0.4-win.jar"
$javaFxFxml = "$env:USERPROFILE\.m2\repository\org\openjfx\javafx-fxml\25.0.4\javafx-fxml-25.0.4-win.jar"
$sqliteJdbc = "$env:USERPROFILE\.m2\repository\org\xerial\sqlite-jdbc\3.53.4.0\sqlite-jdbc-3.53.4.0.jar"

function Find-Maven {
    $mvn = Get-Command "mvn.cmd" -ErrorAction SilentlyContinue
    if ($mvn) {
        return $mvn.Source
    }

    $intellijMaven = "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    if (Test-Path $intellijMaven) {
        return $intellijMaven
    }

    throw "Maven was not found. Open IntelliJ and use its Maven tool window, or install Maven and add it to PATH."
}

function Copy-RequiredJar($path) {
    if (!(Test-Path $path)) {
        throw "Required dependency was not found: $path"
    }

    Copy-Item -LiteralPath $path -Destination $packageInput -Force
}

$jpackage = Get-Command "jpackage" -ErrorAction SilentlyContinue
if (!$jpackage) {
    throw "jpackage was not found. Install/use a full JDK, not a JRE."
}

$maven = Find-Maven

Push-Location $projectRoot
try {
    & $maven package

    New-Item -ItemType Directory -Force -Path $packageInput | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot "target\$appJarName") -Destination $packageInput -Force

    Copy-RequiredJar $javaFxBase
    Copy-RequiredJar $javaFxGraphics
    Copy-RequiredJar $javaFxControls
    Copy-RequiredJar $javaFxFxml
    Copy-RequiredJar $sqliteJdbc

    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    $appImage = Join-Path $dist $appName
    if (Test-Path $appImage) {
        Remove-Item -LiteralPath $appImage -Recurse -Force
    }

    & jpackage `
        --type app-image `
        --name $appName `
        --app-version "1.0" `
        --input $packageInput `
        --main-jar $appJarName `
        --main-class "com.morrello.spacedrepetition.Launcher" `
        --icon $appIcon `
        --dest $dist

    Write-Host ""
    Write-Host "Packaged app:"
    Write-Host (Join-Path $appImage "$appName.exe")
} finally {
    Pop-Location
}
