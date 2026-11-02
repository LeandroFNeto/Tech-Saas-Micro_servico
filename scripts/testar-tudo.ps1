param (
    [switch]$ApenasBackend,
    [switch]$ApenasFrontend
)

$erroGeral = $false

if (-not $ApenasFrontend) {
    Write-Host "==============================" -ForegroundColor Cyan
    Write-Host "Executando testes do Backend..." -ForegroundColor Cyan
    Write-Host "==============================" -ForegroundColor Cyan
    mvn test
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Falha nos testes do Backend!" -ForegroundColor Red
        $erroGeral = $true
    } else {
        Write-Host "Backend validado com sucesso!" -ForegroundColor Green
    }
}

if (-not $ApenasBackend) {
    Write-Host "===============================" -ForegroundColor Cyan
    Write-Host "Executando testes do Frontend..." -ForegroundColor Cyan
    Write-Host "===============================" -ForegroundColor Cyan
    npm --prefix frontend test -- --watch=false --browsers=ChromeHeadless
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Falha nos testes do Frontend!" -ForegroundColor Red
        $erroGeral = $true
    } else {
        Write-Host "Frontend validado com sucesso!" -ForegroundColor Green
    }
}

if ($erroGeral) {
    Write-Host "Bateria de testes finalizada com erros." -ForegroundColor Red
    exit 1
}

Write-Host "Todos os testes foram executados e aprovados com sucesso!" -ForegroundColor Green
exit 0
