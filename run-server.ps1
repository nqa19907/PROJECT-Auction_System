param([int]$Port = 8080)

[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [System.Text.UTF8Encoding]::new($false)
chcp.com 65001 > $null

$env:JAVA_TOOL_OPTIONS = "-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

& "$PSScriptRoot\mvnw.cmd" `
    -f "$PSScriptRoot\AUCTION_SYSTEM\auction\pom.xml" `
    compile `
    exec:java `
    "-Dexec.mainClass=auction_system.server.Launcher" `
    "-Dexec.args=$Port"
