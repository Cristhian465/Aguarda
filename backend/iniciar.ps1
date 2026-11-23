$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)
try {
    $adb = (Get-Command adb.exe -ErrorAction SilentlyContinue).Source
    if (-not $adb) {
        $sdk = $env:ANDROID_HOME
        if (-not $sdk) { $sdk = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
        $adb = Join-Path $sdk 'platform-tools\adb.exe'
    }

    if (Test-Path -LiteralPath $adb) {
        $dispositivos = @(& $adb devices | ForEach-Object {
            if ($_ -match '^(\S+)\s+device\s*$') { $Matches[1] }
        })
        if ($LASTEXITCODE -ne 0) {
            Write-Warning 'No se pudo contactar con ADB para configurar la conexion USB.'
        } elseif ($dispositivos.Count -eq 0) {
            Write-Host 'Para un celular por USB, activa la depuracion y acepta la autorizacion en el telefono. Vuelve a ejecutar este comando con el celular conectado.'
        }
        foreach ($dispositivo in $dispositivos) {
            & $adb -s $dispositivo reverse tcp:8000 tcp:8000
            if ($LASTEXITCODE -ne 0) { Write-Warning 'ADB no pudo conectar el puerto del dispositivo.' }
        }
    } else {
        Write-Host 'Para conectar un celular por USB, instala Android SDK Platform-Tools desde Android Studio.'
    }

    Write-Host 'API de cortes en http://127.0.0.1:8000/docs. Manten abierta esta terminal.'
    python -m uvicorn backend.api:app --host 127.0.0.1 --port 8000
} finally {
    Pop-Location
}
