# Permanent Node.js file blocker for Spring Boot backend
# This script runs continuously to prevent any Node.js contamination

while ($true) {
    # Check and remove Node.js files
    if (Test-Path "server.js") {
        Remove-Item "server.js" -Force
        Write-Host "[BLOCKED] Removed server.js - Spring Boot backend only!" -ForegroundColor Red
    }
    if (Test-Path "routes") {
        Remove-Item "routes" -Recurse -Force
        Write-Host "[BLOCKED] Removed routes directory - Spring Boot backend only!" -ForegroundColor Red
    }
    if (Test-Path "node_modules") {
        Remove-Item "node_modules" -Recurse -Force
        Write-Host "[BLOCKED] Removed node_modules directory - Spring Boot backend only!" -ForegroundColor Red
    }
    if (Test-Path "package.json") {
        Remove-Item "package.json" -Force
        Write-Host "[BLOCKED] Removed package.json - Spring Boot backend only!" -ForegroundColor Red
    }
    if (Test-Path "package-lock.json") {
        Remove-Item "package-lock.json" -Force
        Write-Host "[BLOCKED] Removed package-lock.json - Spring Boot backend only!" -ForegroundColor Red
    }
    
    # Check for any JavaScript files
    Get-ChildItem -Path "." -Filter "*.js" -ErrorAction SilentlyContinue | Where-Object {$_.Name -ne "cleanup.ps1"} | ForEach-Object {
        Remove-Item $_.FullName -Force
        Write-Host "[BLOCKED] Removed $($_.Name) - Spring Boot backend only!" -ForegroundColor Red
    }
    
    Start-Sleep -Seconds 2
}
