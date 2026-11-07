param (
    [switch]$Todos,
    [switch]$ApenasAlterados = $true
)

if ($Todos) {
    $ApenasAlterados = $false
}

$caminhos = @()

if ($ApenasAlterados) {
    $gitStatus = git status --porcelain
    foreach ($linha in $gitStatus) {
        if ($linha.Length -gt 3) {
            $arquivo = $linha.Substring(3).Trim()
            if ($arquivo -match "\.(java|ts|html|css)$" -and (Test-Path $arquivo)) {
                $caminhos += (Get-Item $arquivo).FullName
            }
        }
    }
} else {
    $caminhos = (Get-ChildItem -Recurse -File src, frontend\src | Where-Object {
        $_.Extension -in @(".java", ".ts", ".html", ".css") -and
        $_.FullName -notmatch "\\node_modules\\" -notmatch "\\target\\" -notmatch "\\dist\\"
    }).FullName
}

$erros = @()

foreach ($caminho in $caminhos) {
    $linhas = Get-Content -Path $caminho
    for ($i = 0; $i -lt $linhas.Count; $i++) {
        $linha = $linhas[$i]
        $textoLimpo = $linha -replace "https?://\S+", "" -replace '"[^"]*"', '""' -replace "'[^']*'", "''"
        if ($textoLimpo -match "//" -or $textoLimpo -match "/\*" -or $textoLimpo -match "\*/") {
            $erros += "$($caminho):$($i + 1): $linha"
        }
    }
}

if ($erros.Count -gt 0) {
    Write-Host "Comentarios proibidos encontrados ($($erros.Count)):" -ForegroundColor Red
    $erros | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
    exit 1
}

Write-Host "Codigo limpo validado: nenhum comentario proibido encontrado." -ForegroundColor Green
exit 0
