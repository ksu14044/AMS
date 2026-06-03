# 로컬 MySQL Flyway repair (checksum mismatch 등)
# application-local.yaml 의 datasource 설정을 사용합니다.
# 사용: ams_backend 에서  .\scripts\flyway-repair-local.ps1

$ErrorActionPreference = 'Stop'
$BackendRoot = Resolve-Path (Join-Path $PSScriptRoot '..')
$YamlPath = Join-Path $BackendRoot 'src\main\resources\application-local.yaml'

if (-not (Test-Path $YamlPath)) {
    Write-Error @"
application-local.yaml 이 없습니다.
  Copy-Item src\main\resources\application-local.yaml.example src\main\resources\application-local.yaml
  후 datasource username/password 를 입력하세요.
"@
}

function Get-YamlScalar($Name) {
    $line = Get-Content $YamlPath | Where-Object { $_ -match "^\s*$Name\s*:" } | Select-Object -First 1
    if (-not $line) { return $null }
    return ($line -replace "^\s*$Name\s*:\s*", '').Trim()
}

$url = Get-YamlScalar 'url'
$user = Get-YamlScalar 'username'
$pass = Get-YamlScalar 'password'

if (-not $url -or -not $user -or -not $pass) {
    Write-Error 'application-local.yaml 에 spring.datasource url / username / password 가 필요합니다.'
}

Write-Host "Flyway repair -> $url (user: $user)" -ForegroundColor Cyan
Push-Location $BackendRoot
try {
    & .\mvnw.cmd flyway:repair `
        "-Dflyway.url=$url" `
        "-Dflyway.user=$user" `
        "-Dflyway.password=$pass"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host 'Done. spring-boot:run 으로 기동하세요.' -ForegroundColor Green
}
finally {
    Pop-Location
}
