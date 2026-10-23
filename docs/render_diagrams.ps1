# 将 docs/src/*.html 渲染为 docs/*.png（需要本机安装 Chrome）
$ErrorActionPreference = "Stop"
$docs = $PSScriptRoot
$chrome = "C:\Program Files\Google\Chrome\Application\chrome.exe"
if (-not (Test-Path $chrome)) {
    $chrome = "C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"
}
if (-not (Test-Path $chrome)) {
    throw "未找到 Chrome：$chrome"
}

$jobs = @(
    @{ html = "modules"; w = 1180; h = 1120; out = "功能模块图.png" },
    @{ html = "er1";     w = 900;  h = 1120; out = "ER图-基础数据.png" },
    @{ html = "er2";     w = 1250; h = 1480; out = "ER图-教学数据.png" },
    @{ html = "arch";    w = 1000; h = 1080; out = "部署架构图.png" }
)

foreach ($j in $jobs) {
    $src = Join-Path $docs ("src\" + $j.html + ".html")
    if (-not (Test-Path $src)) {
        throw "缺少 $src，请先执行 python docs/make_diagrams.py"
    }
    $png = Join-Path $docs $j.out
    $profile = Join-Path $env:TEMP ("cms-chrome-" + $j.html)
    Start-Process -FilePath $chrome -Wait -ArgumentList @(
        "--headless=old", "--disable-gpu", "--no-sandbox", "--hide-scrollbars",
        "--force-device-scale-factor=1", "--user-data-dir=$profile",
        "--window-size=$($j.w),$($j.h)", "--screenshot=$png",
        "file:///$($src.Replace('\', '/'))"
    ) -RedirectStandardError (Join-Path $env:TEMP ("cms-" + $j.html + ".err"))
    Write-Host "已生成 $($j.out)"
}
