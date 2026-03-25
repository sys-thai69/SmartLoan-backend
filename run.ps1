Set-Location $PSScriptRoot
$mavenBin = Join-Path $env:TEMP "maven\bin"
$env:Path = "$mavenBin;$env:Path"

Write-Host "Starting WingLoan Backend..."
Write-Host "Maven: $mavenBin"
Write-Host "Using IPv4 preference for database connection"

# Force IPv4 to fix Supabase IPv6-only DNS issue
& mvn spring-boot:run "-Dspring-boot.run.jvmArguments=-Djava.net.preferIPv4Stack=true"
