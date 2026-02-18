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
  -H "Content-Type: multipart/form-data"
```

**For Excel file:**
```bash
curl -X POST http://localhost:8080/api/v1/financial-data/convert \
  -F "file=@path/to/your/financial_data.xlsx" \
  -H "Content-Type: multipart/form-data"
```

**For JSON file:**
```bash
curl -X POST http://localhost:8080/api/v1/financial-data/convert \
  -F "file=@path/to/your/financial_data.json" \
  -H "Content-Type: multipart/form-data"
```

#### Using PowerShell (Windows)

```powershell
$uri = "http://localhost:8080/api/v1/financial-data/convert"
$filePath = "C:\path\to\your\financial_data.csv"
$form = @{
    file = Get-Item -Path $filePath
}
Invoke-RestMethod -Uri $uri -Method Post -Form $form
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

## Step 4: Understanding the Response

The API returns a JSON response with the following structure:

```json
{
  "markdown": "# Financial Data Report\n\n...",
  "filename": "financial_data.csv",
  "recordCount": 10,
  "processedAt": "2026-02-18T10:30:00",
  "fileType": "text/csv",
  "status": "SUCCESS"
}
```

The `markdown` field contains the converted markdown format of your financial data.

## Supported File Formats

- **CSV** (.csv) - Comma-separated values
- **Excel** (.xlsx, .xls) - Microsoft Excel files
- **JSON** (.json) - JavaScript Object Notation
- **TXT** (.txt) - Plain text files (tab/comma/pipe delimited)

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
