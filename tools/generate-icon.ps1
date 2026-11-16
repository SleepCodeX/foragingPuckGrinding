# Original Hunting Helper axe badge. No external artwork.
Add-Type -AssemblyName System.Drawing
$badge = [Drawing.Bitmap]::new(128, 128)
$graphics = [Drawing.Graphics]::FromImage($badge)
$graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.ScaleTransform(4, 4)
function Paint-Shape([string]$hex, [double[]]$coordinates) {
    $points = [Drawing.PointF[]]::new($coordinates.Length / 2)
    for ($i = 0; $i -lt $points.Length; $i++) {
        $points[$i] = [Drawing.PointF]::new($coordinates[$i * 2], $coordinates[$i * 2 + 1])
    }
    $brush = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml($hex))
    try { $graphics.FillPolygon($brush, $points) } finally { $brush.Dispose() }
}
$background = [Drawing.SolidBrush]::new([Drawing.ColorTranslator]::FromHtml('#1D2632'))
$rim = [Drawing.Pen]::new([Drawing.ColorTranslator]::FromHtml('#8672AF'), 0.7)
$graphics.FillEllipse($background, 1, 1, 30, 30)
$graphics.DrawEllipse($rim, 1.4, 1.4, 29.2, 29.2)
Paint-Shape '#17151F' @(8,25, 11,27, 24,10, 20,7)
Paint-Shape '#BE935D' @(8,24, 10,25.5, 23,9, 21,7.5)
Paint-Shape '#E7C081' @(8,24, 8.8,24.5, 22,8, 21,7.5)
Paint-Shape '#71588E' @(15,7, 18,4, 23,5, 27,9, 27,14, 24,18, 20,15, 22,12, 19,9)
Paint-Shape '#C4AFE9' @(16,7, 18.5,5, 23,6, 26,9.5, 26,14, 24,16.5, 21.5,14.5, 23,11.5, 19.5,8)
Paint-Shape '#F3EBDD' @(24,7, 26,9.5, 26,14, 24,16.5, 23,15.5, 25,13.5, 25,10)
Paint-Shape '#8A71C4' @(15,7, 17,5.5, 20,8, 18.5,10)
$target = Join-Path $PSScriptRoot '..\src\main\resources\assets\foragingpuckgrinding\icon.png'
$badge.Save([IO.Path]::GetFullPath($target), [Drawing.Imaging.ImageFormat]::Png)
$rim.Dispose()
$background.Dispose()
$graphics.Dispose()
$badge.Dispose()
