<#
.SYNOPSIS
    ECHO One-Command Live Update Deployment Script
.DESCRIPTION
    Automatically updates the version in build.gradle.kts and version.json,
    commits the changes, creates a Git release tag, and pushes to GitHub.
    GitHub Actions CI/CD will automatically compile, sign, and publish the update.
.EXAMPLE
    .\push-update.ps1 -Version "1.0.1" -Notes "Fixed lock-screen controls and added new songs"
#>

param(
    [Parameter(Mandatory=$true, Position=0)]
    [string]$Version,

    [Parameter(Mandatory=$false, Position=1)]
    [string]$Notes = "Performance improvements and bug fixes."
)

$ErrorActionPreference = "Stop"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "🚀 ECHO Music Player — Live Update Deployment" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan

# Clean version string (e.g. "v1.0.1" -> "1.0.1")
$CleanVersion = $Version.TrimStart('v')
$Tag = "v$CleanVersion"

$GradlePath = "own-app\app\build.gradle.kts"
$VersionJsonPath = "version.json"

if (-not (Test-Path $GradlePath)) {
    Write-Error "Could not find $GradlePath. Please run this script from the project root."
}

# 1. Update version in build.gradle.kts
Write-Host "`n[1/4] Updating version in $GradlePath..." -ForegroundColor Yellow
$GradleContent = Get-Content $GradlePath -Raw

# Increment versionCode
if ($GradleContent -match 'versionCode\s*=\s*(\d+)') {
    $CurrentCode = [int]$matches[1]
    $NewCode = $CurrentCode + 1
    $GradleContent = $GradleContent -replace "versionCode\s*=\s*\d+", "versionCode = $NewCode"
    Write-Host "  • Bumped versionCode: $CurrentCode -> $NewCode" -ForegroundColor Green
} else {
    $NewCode = 2
}

# Update versionName
$GradleContent = $GradleContent -replace 'versionName\s*=\s*"[^"]+"', ('versionName = "{0}"' -f $CleanVersion)
Set-Content -Path $GradlePath -Value $GradleContent -NoNewline
Write-Host "  • Set versionName: $CleanVersion" -ForegroundColor Green

# 2. Update version.json manifest
Write-Host "`n[2/4] Updating $VersionJsonPath..." -ForegroundColor Yellow
$IsoDate = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ")
$JsonContent = @"
{
  "versionCode": $NewCode,
  "versionName": "$CleanVersion",
  "releaseTitle": "ECHO $Tag — Live Update",
  "releaseNotes": "$Notes",
  "downloadUrl": "https://github.com/Deadmonster-a1/ECHO/releases/download/$Tag/echo.apk",
  "apkSize": 4353992,
  "mandatory": false,
  "publishedAt": "$IsoDate"
}
"@
Set-Content -Path $VersionJsonPath -Value $JsonContent
if (Test-Path "own-app\version.json") {
    Set-Content -Path "own-app\version.json" -Value $JsonContent
}
Write-Host "  • Updated version.json manifest" -ForegroundColor Green

# 3. Git Commit
Write-Host "`n[3/4] Committing changes to Git..." -ForegroundColor Yellow
git add -A
git commit -m "release: $Tag — $Notes"
git tag -a $Tag -m "Release $Tag: $Notes"
Write-Host "  • Created Git commit and tag: $Tag" -ForegroundColor Green

# 4. Git Push
Write-Host "`n[4/4] Pushing to GitHub..." -ForegroundColor Yellow
try {
    git push origin main
    git push origin $Tag
    Write-Host "`n==================================================" -ForegroundColor Green
    Write-Host "🎉 SUCCESS! Live update $Tag pushed to GitHub!" -ForegroundColor Green
    Write-Host "• GitHub Actions is now compiling and publishing your release." -ForegroundColor Cyan
    Write-Host "• Active users will receive the update dialog inside the app." -ForegroundColor Cyan
    Write-Host "==================================================" -ForegroundColor Green
} catch {
    Write-Host "`n⚠️ Could not push to remote. Please ensure you have added your GitHub remote:" -ForegroundColor Yellow
    Write-Host "  git remote add origin https://github.com/Deadmonster-a1/ECHO.git" -ForegroundColor White
    Write-Host "Then run: git push origin main --tags" -ForegroundColor White
}
