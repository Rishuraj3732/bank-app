param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9]+(?:[._-][a-z0-9]+)*$')]
    [string]$ImageNamespace,

    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-zA-Z0-9_][a-zA-Z0-9_.-]{0,127}$')]
    [string]$ImageTag,

    [Parameter(Mandatory = $true)]
    [string]$OutputDirectory
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$manifestDirectory = Join-Path $repositoryRoot 'k8s'
if ($OutputDirectory -notmatch '^k8s-deploy-[a-zA-Z0-9_.-]+$') {
    throw 'OutputDirectory must be a generated k8s-deploy-* directory name.'
}
$outputPath = Join-Path $repositoryRoot $OutputDirectory

$images = @{
    'rishuraj1/discoveryservice:latest' = "$ImageNamespace/discoveryservice:$ImageTag"
    'rishuraj1/accountservice:latest' = "$ImageNamespace/accountservice:$ImageTag"
    'rishuraj1/customer-service:latest' = "$ImageNamespace/customer-service:$ImageTag"
    'rishuraj1/identity-service:latest' = "$ImageNamespace/identity-service:$ImageTag"
    'rishuraj1/swiggy-gateway:latest' = "$ImageNamespace/swiggy-gateway:$ImageTag"
    'rishuraj1/bank-frontend:latest' = "$ImageNamespace/bank-frontend:$ImageTag"
}
$replacedImages = @{}
$encoding = New-Object System.Text.UTF8Encoding -ArgumentList $false

if (Test-Path -LiteralPath $outputPath) {
    Remove-Item -LiteralPath $outputPath -Recurse -Force
}
New-Item -Path $outputPath -ItemType Directory | Out-Null

foreach ($fileName in @('04-discovery.yaml', '05-services.yaml', '06-frontend.yaml')) {
    $sourcePath = Join-Path $manifestDirectory $fileName
    $content = [System.IO.File]::ReadAllText($sourcePath)
    foreach ($image in $images.GetEnumerator()) {
        if ($content.Contains($image.Key)) {
            $content = $content.Replace($image.Key, $image.Value)
            $replacedImages[$image.Key] = $true
        }
    }
    [System.IO.File]::WriteAllText((Join-Path $outputPath $fileName), $content, $encoding)
}

$missingImages = @($images.Keys | Where-Object { -not $replacedImages.ContainsKey($_) })
if ($missingImages.Count -gt 0) {
    Remove-Item -LiteralPath $outputPath -Recurse -Force
    throw "Kubernetes manifests are missing expected Docker Hub image references: $($missingImages -join ', ')"
}
