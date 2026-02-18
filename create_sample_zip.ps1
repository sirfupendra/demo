# PowerShell script to create a sample ZIP file for testing

Write-Host "Creating sample ZIP file for testing..." -ForegroundColor Green

# Check if sample files exist
if (-not (Test-Path "sample_financial_data.csv")) {
    Write-Host "Error: sample_financial_data.csv not found" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path "sample_financial_data.json")) {
    Write-Host "Error: sample_financial_data.json not found" -ForegroundColor Red
    exit 1
}

# Create ZIP file
$zipPath = "sample_financial_data.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}

# Create ZIP using .NET compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open($zipPath, [System.IO.Compression.ZipArchiveMode]::Create)

# Add CSV file
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "sample_financial_data.csv", "financial_data_january.csv")

# Add JSON file
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, "sample_financial_data.json", "financial_data_february.json")

# Create a second CSV with different data
$csv2Content = @"
Date,Amount,Description,Category,Account
2026-02-01,3000.00,Bonus Payment,Income,Checking Account
2026-02-05,-200.00,Utility Bill,Expense,Checking Account
2026-02-10,-150.00,Internet Bill,Expense,Credit Card
"@
$csv2Path = "temp_financial_data2.csv"
$csv2Content | Out-File -FilePath $csv2Path -Encoding UTF8
[System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $csv2Path, "financial_data_march.csv")
Remove-Item $csv2Path -Force

$zip.Dispose()

Write-Host "✓ Sample ZIP file created: $zipPath" -ForegroundColor Green
Write-Host ""
Write-Host "ZIP file contains:" -ForegroundColor Yellow
Write-Host "  - financial_data_january.csv" -ForegroundColor Cyan
Write-Host "  - financial_data_february.json" -ForegroundColor Cyan
Write-Host "  - financial_data_march.csv" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now test the API with this ZIP file!" -ForegroundColor Green
