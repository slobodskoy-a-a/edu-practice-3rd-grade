param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $MavenArguments
)

$javac = Get-Command javac -ErrorAction Stop
$env:JAVA_HOME = Split-Path (Split-Path $javac.Source -Parent) -Parent
$maven = Join-Path $PSScriptRoot '..\.tools\apache-maven-3.10.1\bin\mvn.cmd'

if (-not (Test-Path -LiteralPath $maven)) {
    throw 'Local Maven was not found in .tools.'
}

& $maven @MavenArguments
exit $LASTEXITCODE
