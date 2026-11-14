# =====================================================================
# LASTDROP - one-click database setup
#
# Run from PowerShell:
#   powershell -ExecutionPolicy Bypass -File "C:\Users\Mani\OneDrive\Desktop\riverse aunction\database\setup-database.ps1"
#
# It will ask for your MySQL ROOT password once (step 1).
# Re-running it is safe: it resets the demo data to a fresh state.
# =====================================================================

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
Set-Location $PSScriptRoot   # run the .sql files from this folder

function Fail($message) {
    Write-Host ""
    Write-Host "FAILED: $message" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $mysql)) {
    Fail "mysql.exe not found at $mysql. Is MySQL Server 8.0 installed?"
}

$service = Get-Service -Name "MySQL80" -ErrorAction SilentlyContinue
if ($null -eq $service -or $service.Status -ne "Running") {
    Fail "The MySQL80 service is not running. Open 'Services', find MySQL80 and click Start."
}

Write-Host "Step 1/3: creating database and app user." -ForegroundColor Cyan
Write-Host "          Type your MySQL ROOT password and press Enter (nothing appears while typing)."
& $mysql -u root -p -e "source 01-create-database.sql"
if ($LASTEXITCODE -ne 0) {
    Fail "Root login failed. Check the root password you chose when installing MySQL and run this script again."
}

# The app user's local password, passed silently so mysql does not print a warning
$env:MYSQL_PWD = "lastdrop_dev"

Write-Host "Step 2/3: creating tables." -ForegroundColor Cyan
& $mysql -u lastdrop --default-character-set=utf8mb4 -e "source 02-schema.sql"
if ($LASTEXITCODE -ne 0) { Fail "Could not create the tables (see the error above)." }

Write-Host "Step 3/3: loading demo data." -ForegroundColor Cyan
& $mysql -u lastdrop --default-character-set=utf8mb4 -e "source 03-sample-data.sql"
if ($LASTEXITCODE -ne 0) { Fail "Could not load the demo data (see the error above)." }

$count = & $mysql -u lastdrop -N -B lastdrop -e "SELECT COUNT(*) FROM products;"
Remove-Item Env:\MYSQL_PWD

Write-Host ""
Write-Host "SUCCESS: database 'lastdrop' is ready with $count products." -ForegroundColor Green
