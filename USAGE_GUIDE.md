# Financial Data to Markdown API - Usage Guide

## Prerequisites

- Java 25 (or compatible version)
- Maven 3.6+ (or use the included Maven wrapper)
- Internet connection (for downloading dependencies)

## Step 1: Build the Project

Open a terminal in the project root directory and run:

```bash
# Using Maven wrapper (Windows)
.\mvnw.cmd clean install

# Using Maven wrapper (Linux/Mac)
./mvnw clean install

# Or if you have Maven installed globally
mvn clean install
```

This will download all dependencies and compile the project.

## Step 2: Run the Application

```bash
# Using Maven wrapper (Windows)
.\mvnw.cmd spring-boot:run

# Using Maven wrapper (Linux/Mac)
./mvnw spring-boot:run

# Or if you have Maven installed globally
mvn spring-boot:run
```

The application will start on **http://localhost:8080**

You should see output like:
```
Started DemoApplication in X.XXX seconds
```

## Step 3: Test the API

### Health Check

First, verify the API is running:

```bash
curl http://localhost:8080/api/v1/financial-data/health
```

Expected response: `Financial Data API is running`

### Convert Financial Data File

#### Using cURL (Command Line)

**For CSV file:**
```bash
curl -X POST http://localhost:8080/api/v1/financial-data/convert \
  -F "file=@path/to/your/financial_data.csv" \
  -H "Content-Type: multipart/form-data" \
  -o financial_report.md
```

**For Excel file:**
```bash
curl -X POST http://localhost:8080/api/v1/financial-data/convert \
  -F "file=@path/to/your/financial_data.xlsx" \
  -H "Content-Type: multipart/form-data" \
  -o financial_report.md
```

**For JSON file:**
```bash
curl -X POST http://localhost:8080/api/v1/financial-data/convert \
  -F "file=@path/to/your/financial_data.json" \
  -H "Content-Type: multipart/form-data" \
  -o financial_report.md
```

**For ZIP file containing multiple files:**
```bash
curl -X POST http://localhost:8080/api/v1/financial-data/convert \
  -F "file=@path/to/your/financial_data.zip" \
  -H "Content-Type: multipart/form-data" \
  -o financial_report.md
```

**Note**: The `-o` flag saves the downloaded markdown file. Without it, the content will be printed to stdout.

#### Using PowerShell (Windows)

```powershell
$uri = "http://localhost:8080/api/v1/financial-data/convert"
$filePath = "C:\path\to\your\financial_data.csv"
$form = @{
    file = Get-Item -Path $filePath
}
$response = Invoke-WebRequest -Uri $uri -Method Post -Form $form

# Extract filename from Content-Disposition header
$contentDisposition = $response.Headers['Content-Disposition']
$filename = "financial_report.md"
if ($contentDisposition -match 'filename="([^"]+)"') {
    $filename = $matches[1]
}

# Save the downloaded file
$response.Content | Out-File -FilePath $filename -Encoding UTF8
Write-Host "File saved as: $filename"
```

#### Using Postman

1. Open Postman
2. Create a new POST request
3. URL: `http://localhost:8080/api/v1/financial-data/convert`
4. Go to **Body** tab
5. Select **form-data**
6. Add a key named `file` and change type to **File**
7. Select your financial data file
8. Click **Send**
9. The response will be a downloadable markdown file - click **Send and Download** to save it automatically

## Step 4: Understanding the Response

The API returns a **downloadable markdown file** (.md) instead of JSON. The response includes:

- **Content-Type**: `text/markdown; charset=utf-8`
- **Content-Disposition**: `attachment; filename="[filename]_report_[timestamp].md"`
- **Body**: The markdown file content

### Response Headers

```
Content-Type: text/markdown; charset=utf-8
Content-Disposition: attachment; filename="financial_data_report_20260218_103045.md"
Content-Length: [file size in bytes]
```

### File Naming Convention

The downloaded markdown file follows this naming pattern:
- **Single files**: `[original_filename]_report_[timestamp].md`
  - Example: `financial_data.csv` → `financial_data_report_20260218_103045.md`
- **ZIP files**: `[zip_filename]_report_[timestamp].md`
  - Example: `financial_data.zip` → `financial_data_report_20260218_103045.md`

The timestamp format is `yyyyMMdd_HHmmss` to ensure unique filenames.

### Download Behavior

- **Browser**: The file will automatically download
- **cURL**: Use `-O` or `-o filename.md` to save the file
- **PowerShell**: The file content is returned and can be saved to disk

## Supported File Formats

- **CSV** (.csv) - Comma-separated values
- **Excel** (.xlsx, .xls) - Microsoft Excel files
- **JSON** (.json) - JavaScript Object Notation
- **TXT** (.txt) - Plain text files (tab/comma/pipe delimited)
- **ZIP** (.zip) - ZIP archives containing multiple financial data files (CSV, Excel, JSON, or TXT)

## ZIP File Support

The API can process ZIP archives containing multiple financial data files. The ZIP file can contain:
- Multiple CSV files
- Multiple Excel files (.xlsx, .xls)
- Multiple JSON files
- Multiple TXT files
- A mix of different file types

**How it works:**
1. Upload a ZIP file containing your financial data files
2. The API extracts all files from the ZIP
3. Each file is processed according to its type
4. All records are combined into a single markdown report
5. The response includes details about each file processed

**Benefits:**
- Process multiple files in one request
- Combine data from different sources
- Get a unified report with all financial data

## File Format Examples

### CSV Format Example

```csv
Date,Amount,Description,Category,Account
2026-01-01,1000.00,Salary,Income,Checking
2026-01-02,-50.00,Grocery Store,Expense,Checking
2026-01-03,-25.00,Gas Station,Expense,Credit Card
```

### JSON Format Example

```json
[
  {
    "Date": "2026-01-01",
    "Amount": 1000.00,
    "Description": "Salary",
    "Category": "Income",
    "Account": "Checking"
  },
  {
    "Date": "2026-01-02",
    "Amount": -50.00,
    "Description": "Grocery Store",
    "Category": "Expense",
    "Account": "Checking"
  }
]
```

## Error Handling

The API provides detailed error messages:

- **400 Bad Request**: File processing error (empty file, invalid format)
- **413 Payload Too Large**: File exceeds 50MB limit
- **415 Unsupported Media Type**: File format not supported
- **500 Internal Server Error**: Unexpected server error

## Configuration

File upload limits are configured in `application.properties`:
- Maximum file size: 50MB
- Maximum request size: 50MB

You can modify these values if needed.

## Troubleshooting

### Port Already in Use

If port 8080 is already in use, change it in `application.properties`:
```properties
server.port=8081
```

### Dependencies Not Downloading

Make sure you have internet connection and Maven can access repositories.

### File Not Found Error

Make sure the file path in your curl command is correct and the file exists.

## Next Steps

- Test with your own financial data files
- Integrate the API into your application
- Customize the markdown output format if needed
