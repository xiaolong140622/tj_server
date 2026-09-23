Add-Type -AssemblyName System.Drawing
$out = "D:\work\tj_server\miniapp\src\static\tab"
$s = 81
$colors = @{ normal = [System.Drawing.Color]::FromArgb(255,153,153,153); active = [System.Drawing.Color]::FromArgb(255,255,107,53) }

function New-Bitmap { param($c, $name)
  $bmp = New-Object System.Drawing.Bitmap($s, $s)
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  $pen = New-Object System.Drawing.Pen($c, 6)
  $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
  $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
  $brush = New-Object System.Drawing.SolidBrush($c)

  switch ($name) {
    'home' {
      $roof = @(
        (New-Object System.Drawing.PointF(10,40)),
        (New-Object System.Drawing.PointF(40,12)),
        (New-Object System.Drawing.PointF(70,40))
      )
      $g.DrawLines($pen, $roof)
      $g.DrawRectangle($pen, 20, 40, 40, 28)
      $g.FillRectangle($brush, 35, 50, 12, 18)
    }
    'order' {
      $path = New-Object System.Drawing.Drawing2D.GraphicsPath
      $path.AddArc(18, 8, 12, 12, 180, 90)
      $path.AddArc(50, 8, 12, 12, 270, 90)
      $path.AddArc(50, 58, 12, 12, 0, 90)
      $path.AddArc(18, 58, 12, 12, 90, 90)
      $path.CloseFigure()
      $g.DrawPath($pen, $path)
      $g.DrawLine($pen, 28, 26, 52, 26)
      $g.DrawLine($pen, 28, 38, 52, 38)
      $g.DrawLine($pen, 28, 50, 44, 50)
    }
    'spread' {
      $g.FillEllipse($brush, 8, 32, 18, 18)
      $g.FillEllipse($brush, 52, 10, 18, 18)
      $g.FillEllipse($brush, 52, 52, 18, 18)
      $g.DrawLine($pen, 22, 40, 58, 20)
      $g.DrawLine($pen, 22, 44, 58, 60)
    }
    'user' {
      $g.FillEllipse($brush, 28, 8, 24, 24)
      $path = New-Object System.Drawing.Drawing2D.GraphicsPath
      $path.AddArc(14, 40, 52, 52, 180, 180)
      $g.FillPath($brush, $path)
    }
  }
  $g.Dispose()
  $bmp.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
}

foreach ($state in 'normal','active') {
  $c = $colors[$state]
  foreach ($name in 'home','order','spread','user') {
    $suffix = if ($state -eq 'active') { "$name-active.png" } else { "$name.png" }
    $bmp = New-Object System.Drawing.Bitmap($s, $s)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $pen = New-Object System.Drawing.Pen($c, 6)
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $brush = New-Object System.Drawing.SolidBrush($c)

    switch ($name) {
      'home' {
        $roof = @(
          (New-Object System.Drawing.PointF(10,40)),
          (New-Object System.Drawing.PointF(40,12)),
          (New-Object System.Drawing.PointF(70,40))
        )
        $g.DrawLines($pen, $roof)
        $g.DrawRectangle($pen, 20, 40, 40, 28)
        $g.FillRectangle($brush, 35, 50, 12, 18)
      }
      'order' {
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddArc(18, 8, 12, 12, 180, 90)
        $path.AddArc(50, 8, 12, 12, 270, 90)
        $path.AddArc(50, 58, 12, 12, 0, 90)
        $path.AddArc(18, 58, 12, 12, 90, 90)
        $path.CloseFigure()
        $g.DrawPath($pen, $path)
        $g.DrawLine($pen, 28, 26, 52, 26)
        $g.DrawLine($pen, 28, 38, 52, 38)
        $g.DrawLine($pen, 28, 50, 44, 50)
      }
      'spread' {
        $g.FillEllipse($brush, 8, 32, 18, 18)
        $g.FillEllipse($brush, 52, 10, 18, 18)
        $g.FillEllipse($brush, 52, 52, 18, 18)
        $g.DrawLine($pen, 22, 40, 58, 20)
        $g.DrawLine($pen, 22, 44, 58, 60)
      }
      'user' {
        $g.FillEllipse($brush, 28, 8, 24, 24)
        $path = New-Object System.Drawing.Drawing2D.GraphicsPath
        $path.AddArc(14, 40, 52, 52, 180, 180)
        $g.FillPath($brush, $path)
      }
    }
    $g.Dispose()
    $bmp.Save((Join-Path $out $suffix), [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
    Write-Host "generated $suffix"
  }
}
