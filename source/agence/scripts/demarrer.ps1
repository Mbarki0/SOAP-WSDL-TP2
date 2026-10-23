param(
    [string]$Configuration = 'agence.properties',
    [switch]$SansSecret
)
$ErrorActionPreference = 'Stop'
$agenceRoot = Split-Path -Parent $PSScriptRoot
$jar = Join-Path $agenceRoot 'target\agence-soap-1.0-SNAPSHOT.jar'
if (-not (Test-Path -LiteralPath $jar)) { throw 'Construisez le projet agence avec mvn package.' }
$javaExecutable = $null
if ($env:JAVA_HOME) {
    $candidat = Join-Path $env:JAVA_HOME 'bin\java.exe'
    if (Test-Path -LiteralPath $candidat) { $javaExecutable = $candidat }
}
if (-not $javaExecutable) {
    $candidat = Join-Path $env:ProgramFiles 'Java\jdk-23\bin\java.exe'
    if (Test-Path -LiteralPath $candidat) { $javaExecutable = $candidat }
}
if (-not $javaExecutable) { $javaExecutable = (Get-Command java.exe -CommandType Application -ErrorAction Stop).Source }
if ($SansSecret) {
    & $javaExecutable -jar $jar $Configuration
    exit $LASTEXITCODE
}
$ancienSecret = $env:AGENCE_SECRET
$secretSaisi = Read-Host 'Secret FICTIF AG-ALPHA fourni par A (Entree pour rechercher seulement)' -AsSecureString
$adresseSecret = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secretSaisi)
try {
    $env:AGENCE_SECRET = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($adresseSecret)
    & $javaExecutable -jar $jar $Configuration
} finally {
    $env:AGENCE_SECRET = $ancienSecret
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($adresseSecret)
    $secretSaisi.Dispose()
}
