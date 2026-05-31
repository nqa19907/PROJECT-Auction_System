param(
    [string]$HostIp = "127.0.0.1",
    [int]$Port = 8080
)

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp.com 65001 > $null

$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
$env:AUCTION_SERVER_HOST = $HostIp
$env:AUCTION_SERVER_PORT = "$Port"

& "$PSScriptRoot\mvnw.cmd" -f "$PSScriptRoot\AUCTION_SYSTEM\auction\pom.xml" javafx:run
