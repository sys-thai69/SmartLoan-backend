Set-Location $PSScriptRoot

# Load .env file
if (Test-Path ".env") {
    Write-Host "Loading environment variables from .env..."
    Get-Content ".env" | ForEach-Object {
        if ($_ -match "^\s*([^#][^=]+)=(.*)$") {
            $name = $matches[1].Trim()
            $value = $matches[2].Trim()
            [Environment]::SetEnvironmentVariable($name, $value, "Process")
            Write-Host "  $name=***"
        }
    }
}

$mavenBin = Join-Path $env:TEMP "maven\bin"
$env:Path = "$mavenBin;$env:Path"

Write-Host ""
Write-Host "Starting WingLoan Backend..."
Write-Host "Maven: $mavenBin"
Write-Host ""

& mvn spring-boot:run
