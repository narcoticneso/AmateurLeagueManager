param([ValidateSet('run', 'dev', 'test', 'package')][string]$Task = 'run')
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    if (-not $env:JAVA_HOME -and -not (Get-Command java -ErrorAction SilentlyContinue)) {
        $localJdk = Get-ChildItem -LiteralPath '.tools' -Directory -Filter 'jdk-*' -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $localJdk) { throw 'Install a Java 21 JDK and set JAVA_HOME, then run this script again.' }
        $env:JAVA_HOME = $localJdk.FullName
    }
    $mavenArgs = switch ($Task) { 'run' { @('spring-boot:run') } 'dev' { @('spring-boot:run', '-Pdev') } default { @($Task) } }
    $localMaven = Join-Path $PSScriptRoot '.tools/apache-maven-3.9.9/bin/mvn.cmd'
    if (Test-Path -LiteralPath $localMaven) {
        & $localMaven '--no-transfer-progress' '-Dmaven.repo.local=.tools/m2' @mavenArgs
    } else {
        & '.\mvnw.cmd' '--no-transfer-progress' @mavenArgs
    }
    $result = $LASTEXITCODE
} finally { Pop-Location }
exit $result
