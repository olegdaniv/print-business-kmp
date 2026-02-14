param(
    [Parameter(Mandatory = $true)]
    [string]$Version,

    [Parameter(Mandatory = $true)]
    [string]$Notes,

    [Parameter(Mandatory = $true)]
    [string]$MsiUrl,

    [Parameter(Mandatory = $true)]
    [string]$Sha256,

    [string]$OutputPath = "latest.json"
)

$feed = @{
    version = $Version
    notes = $Notes
    windows = @{
        url = $MsiUrl
        sha256 = $Sha256
    }
}

$json = $feed | ConvertTo-Json -Depth 5
Set-Content -Path $OutputPath -Value $json -Encoding utf8NoBOM
Write-Host "Wrote $OutputPath"
