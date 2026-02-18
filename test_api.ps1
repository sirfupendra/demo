# PowerShell script to test the Financial Data API

Write-Host "=== Financial Data to Markdown API Test ===" -ForegroundColor Green
Write-Host ""

# Check if the API is running
Write-Host "1. Testing Health Endpoint..." -ForegroundColor Yellow
try {
    $healthResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/financial-data/health" -Method Get
    Write-Host "✓ Health Check: $healthResponse" -ForegroundColor Green
} catch {
    Write-Host "✗ API is not running. Please start the Spring Boot application first." -ForegroundColor Red
    Write-Host "  Run: .\mvnw.cmd spring-boot:run" -ForegroundColor Yellow
    exit 1
}

Write-Host ""

# Test CSV file conversion
Write-Host "2. Testing CSV File Conversion..." -ForegroundColor Yellow
if (Test-Path "sample_financial_data.csv") {
    try {
        $uri = "http://localhost:8080/api/v1/financial-data/convert"
        $form = @{
            file = Get-Item -Path "sample_financial_data.csv"
        }
        $response = Invoke-RestMethod -Uri $uri -Method Post -Form $form
        
        Write-Host "✓ CSV Conversion Successful!" -ForegroundColor Green
        Write-Host "  Filename: $($response.filename)" -ForegroundColor Cyan
        Write-Host "  Records Processed: $($response.recordCount)" -ForegroundColor Cyan
        Write-Host "  Status: $($response.status)" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Markdown Preview (first 500 characters):" -ForegroundColor Yellow
        Write-Host $response.markdown.Substring(0, [Math]::Min(500, $response.markdown.Length)) -ForegroundColor White
        Write-Host "..."
        
        # Save markdown to file
        $response.markdown | Out-File -FilePath "output_financial_report.md" -Encoding UTF8
        Write-Host ""
        Write-Host "✓ Full markdown saved to: output_financial_report.md" -ForegroundColor Green
    } catch {
        Write-Host "✗ CSV Conversion Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Sample CSV file not found: sample_financial_data.csv" -ForegroundColor Red
}

Write-Host ""

# Test JSON file conversion
Write-Host "3. Testing JSON File Conversion..." -ForegroundColor Yellow
if (Test-Path "sample_financial_data.json") {
    try {
        $uri = "http://localhost:8080/api/v1/financial-data/convert"
        $form = @{
            file = Get-Item -Path "sample_financial_data.json"
        }
        $response = Invoke-RestMethod -Uri $uri -Method Post -Form $form
        
        Write-Host "✓ JSON Conversion Successful!" -ForegroundColor Green
        Write-Host "  Filename: $($response.filename)" -ForegroundColor Cyan
        Write-Host "  Records Processed: $($response.recordCount)" -ForegroundColor Cyan
        Write-Host "  Status: $($response.status)" -ForegroundColor Cyan
    } catch {
        Write-Host "✗ JSON Conversion Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Sample JSON file not found: sample_financial_data.json" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green
