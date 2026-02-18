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
        $response = Invoke-WebRequest -Uri $uri -Method Post -Form $form
        
        # Extract filename from Content-Disposition header
        $contentDisposition = $response.Headers['Content-Disposition']
        $filename = "output_financial_report.md"
        if ($contentDisposition -match 'filename="([^"]+)"') {
            $filename = $matches[1]
        }
        
        # Save the downloaded file
        $response.Content | Out-File -FilePath $filename -Encoding UTF8
        
        Write-Host "✓ CSV Conversion Successful!" -ForegroundColor Green
        Write-Host "  Downloaded file: $filename" -ForegroundColor Cyan
        Write-Host "  File size: $($response.Content.Length) bytes" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Markdown Preview (first 500 characters):" -ForegroundColor Yellow
        Write-Host $response.Content.Substring(0, [Math]::Min(500, $response.Content.Length)) -ForegroundColor White
        Write-Host "..."
        Write-Host ""
        Write-Host "✓ Markdown file saved: $filename" -ForegroundColor Green
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
        $response = Invoke-WebRequest -Uri $uri -Method Post -Form $form
        
        # Extract filename from Content-Disposition header
        $contentDisposition = $response.Headers['Content-Disposition']
        $filename = "output_financial_report.json.md"
        if ($contentDisposition -match 'filename="([^"]+)"') {
            $filename = $matches[1]
        }
        
        # Save the downloaded file
        $response.Content | Out-File -FilePath $filename -Encoding UTF8
        
        Write-Host "✓ JSON Conversion Successful!" -ForegroundColor Green
        Write-Host "  Downloaded file: $filename" -ForegroundColor Cyan
        Write-Host "  File size: $($response.Content.Length) bytes" -ForegroundColor Cyan
    } catch {
        Write-Host "✗ JSON Conversion Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "✗ Sample JSON file not found: sample_financial_data.json" -ForegroundColor Red
}

Write-Host ""

# Test ZIP file conversion
Write-Host "4. Testing ZIP File Conversion..." -ForegroundColor Yellow
if (Test-Path "sample_financial_data.zip") {
    try {
        $uri = "http://localhost:8080/api/v1/financial-data/convert"
        $form = @{
            file = Get-Item -Path "sample_financial_data.zip"
        }
        $response = Invoke-WebRequest -Uri $uri -Method Post -Form $form
        
        # Extract filename from Content-Disposition header
        $contentDisposition = $response.Headers['Content-Disposition']
        $filename = "output_zip_financial_report.md"
        if ($contentDisposition -match 'filename="([^"]+)"') {
            $filename = $matches[1]
        }
        
        # Save the downloaded file
        $response.Content | Out-File -FilePath $filename -Encoding UTF8
        
        Write-Host "✓ ZIP Conversion Successful!" -ForegroundColor Green
        Write-Host "  Downloaded file: $filename" -ForegroundColor Cyan
        Write-Host "  File size: $($response.Content.Length) bytes" -ForegroundColor Cyan
        Write-Host ""
        Write-Host "Markdown Preview (first 500 characters):" -ForegroundColor Yellow
        Write-Host $response.Content.Substring(0, [Math]::Min(500, $response.Content.Length)) -ForegroundColor White
        Write-Host "..."
        Write-Host ""
        Write-Host "✓ ZIP markdown file saved: $filename" -ForegroundColor Green
    } catch {
        Write-Host "✗ ZIP Conversion Failed: $($_.Exception.Message)" -ForegroundColor Red
    }
} else {
    Write-Host "⚠ Sample ZIP file not found: sample_financial_data.zip" -ForegroundColor Yellow
    Write-Host "  Run .\create_sample_zip.ps1 to create a sample ZIP file" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Test Complete ===" -ForegroundColor Green
