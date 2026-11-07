param (
    [switch]$ComRegressao
)

$falhas = @()

$arquivosTesteJava = Get-ChildItem -Recurse -File -Path "src\test\java" -Filter "*Test.java"
foreach ($arq in $arquivosTesteJava) {
    $conteudo = Get-Content -Raw -Path $arq.FullName
    $vazioMatches = [regex]::Matches($conteudo, '@Test[\s\S]{0,220}?\bvoid\s+(\w+)\s*\([^)]*\)\s*(?:throws\s+[\w,\s]+)?\{\s*\}')
    foreach ($m in $vazioMatches) {
        $falhas += "Teste Java vazio sem assercao: $($arq.FullName) -> $($m.Groups[1].Value)"
    }

    if ($conteudo -match '@Disabled\b') {
        $falhas += "Teste Java desabilitado (@Disabled): $($arq.FullName)"
    }

    $linhas = Get-Content -Path $arq.FullName
    for ($i = 0; $i -lt $linhas.Count; $i++) {
        if ($linhas[$i] -match '^\s*(?:public\s+)?void\s+(deve\w+|test\w+)\s*\(') {
            $nomeMetodo = $Matches[1]
            $inicioBusca = [Math]::Max(0, $i - 6)
            $blocoAnterior = ($linhas[$inicioBusca..$i] -join "`n")
            if ($blocoAnterior -notmatch '@(Test|ParameterizedTest|RepeatedTest)\b') {
                $falhas += "Metodo de teste Java sem @Test: $($arq.FullName):$($i + 1) -> $nomeMetodo"
            }
        }
    }
}

$unidadesFront = Get-ChildItem -Recurse -File -Path "frontend\src\app" | Where-Object {
    $_.Name -match '\.(component|service|guard|interceptor)\.ts$' -and $_.Name -notmatch '\.spec\.ts$'
}
foreach ($unidade in $unidadesFront) {
    $specEsperado = $unidade.FullName -replace '\.ts$', '.spec.ts'
    if (-not (Test-Path $specEsperado)) {
        $falhas += "Unidade Angular sem arquivo .spec.ts: $($unidade.FullName)"
    }
}

$specsFront = Get-ChildItem -Recurse -File -Path "frontend\src\app" -Filter "*.spec.ts"
foreach ($spec in $specsFront) {
    $conteudoSpec = Get-Content -Raw -Path $spec.FullName
    if ($conteudoSpec -match '\b(fdescribe|fit|xdescribe|xit)\s*\(') {
        $falhas += "Spec Angular focada ou ignorada ($($Matches[1])): $($spec.FullName)"
    }
}

powershell -ExecutionPolicy Bypass -File "scripts\verificar-codigo-limpo.ps1"
if ($LASTEXITCODE -ne 0) {
    $falhas += "Falha na verificacao de codigo limpo (comentarios proibidos nos arquivos alterados)."
}

if ($falhas.Count -gt 0) {
    Write-Host "Auditoria Kirito encontrou $($falhas.Count) problema(s):" -ForegroundColor Red
    foreach ($f in $falhas) {
        Write-Host "  - $f" -ForegroundColor Yellow
    }
    exit 1
}

Write-Host "Auditoria estatica Kirito aprovada (zero testes vazios, zero specs ausentes, codigo limpo)." -ForegroundColor Green

if ($ComRegressao) {
    powershell -ExecutionPolicy Bypass -File "scripts\testar-tudo.ps1"
    if ($LASTEXITCODE -ne 0) {
        exit 1
    }
}

exit 0
