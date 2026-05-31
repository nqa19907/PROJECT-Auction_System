param([int]$Port = 8080)

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp.com 65001 > $null

$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
$env:AUCTION_SERVER_PORT = "$Port"

$jar = Get-ChildItem -Path $PSScriptRoot -Filter "auction-*-server.jar" |
    Select-Object -First 1

if ($null -eq $jar) {
    throw "Không tìm thấy file auction-*-server.jar trong $PSScriptRoot"
}

& java -jar $jar.FullName
