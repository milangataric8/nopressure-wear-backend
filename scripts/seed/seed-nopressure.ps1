[CmdletBinding()]
param(
    [string]$MediaRoot = 'C:\milan\projects_media_&_vectors\no_pressure',
    [string]$UploadDir = (Join-Path $PSScriptRoot '../../uploads/products'),
    [string]$Psql = 'C:\Program Files\PostgreSQL\18\bin\psql.exe',
    [string]$DatabaseHost = 'localhost',
    [int]$Port = 5432,
    [string]$Database = 'nopressure_wear',
    [string]$DatabaseUser = 'postgres',
    [switch]$PrepareOnly
)
$ErrorActionPreference = 'Stop'
$models = @(
    @{ Folder = 'nopressure_moto'; Prefix = 'nopressure_moto_tshirt' },
    @{ Folder = 'nopressure_row'; Prefix = 'nopressure_c_row' },
    @{ Folder = 'nopressure_signature'; Prefix = 'nopressure_signature_tshirt' },
    @{ Folder = 'nopressure_circle'; Prefix = 'nopressure_circle_tshirt' }
)
$colors = @('black', 'green', 'ivory', 'navyblue', 'white')
$files = @(foreach ($model in $models) {
    foreach ($color in $colors) {
        $name = "$($model.Prefix)_$color.jpg"
        $source = Join-Path (Join-Path $MediaRoot $model.Folder) $name
        if (-not (Test-Path -LiteralPath $source -PathType Leaf)) {
            throw "Image missing: $source"
        }
        [pscustomobject]@{ Source = $source; Name = $name }
    }
})
if (-not $PrepareOnly -and -not (Get-Command $Psql -ErrorAction SilentlyContinue)) {
    throw "psql not found: $Psql. Supply -Psql with the installed executable path."
}
# Validate every source before copying. Existing identical assets are left alone;
# a different image with the same name is never silently overwritten.
$destination = [IO.Path]::GetFullPath($UploadDir)
foreach ($file in $files) {
    $target = Join-Path $destination $file.Name
    if ((Test-Path -LiteralPath $target) -and
        (Get-FileHash -LiteralPath $target).Hash -ne (Get-FileHash -LiteralPath $file.Source).Hash) {
        throw "Different image already exists: $target"
    }
}
New-Item -ItemType Directory -Path $destination -Force | Out-Null
foreach ($file in $files) {
    $target = Join-Path $destination $file.Name
    if (-not (Test-Path -LiteralPath $target)) {
        Copy-Item -LiteralPath $file.Source -Destination $target
    }
}
Write-Host "Prepared $($files.Count) images in $destination"
if ($PrepareOnly) {
    Write-Host 'PrepareOnly: no database connection or changes.'
    return
}
$sqlPath = Join-Path $PSScriptRoot 'nopressure.sql'
& $Psql -X --host=$DatabaseHost --port=$Port --username=$DatabaseUser --dbname=$Database `
    --set=ON_ERROR_STOP=1 "--file=$sqlPath"
if ($LASTEXITCODE -ne 0) {
    throw 'Database seed failed and its transaction was rolled back. Prepared images remain available for retry.'
}
Write-Host 'Seed completed: SUPER_ADMIN, 20 products, 80 sizes, and color links.'
