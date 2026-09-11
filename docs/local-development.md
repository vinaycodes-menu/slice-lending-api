// to set DB_password for the enviroment variable in power shel

$dbSecure = Read-Host "Enter PostgreSQL password for slice_app" -AsSecureString 

$dbCredential = [pscredential]::new("slice_app", $dbSecure)
$env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
//
if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
   "DB_PASSWORD is missing"
} else {
    "DB_PASSWORD is set"
}

// To secure postgresql password

$dbSecurePassword = Read-Host "PostgreSQL password for slice_app" -AsSecureString
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new("", $dbSecurePassword).Password


// TO LOGIN WE PASS THE SECURE 32 SIGNING KEY IN THE POWERSHELL AND RUN THIS COMMAND.


// TO VERIFY SIGNING KEY IS WORKING OR NOT USE THIS, THE RESULTS MUST SEE 44.
$env:JWT_SIGNING_KEY.Length


//Then generate/set the JWT key in the same PowerShell window:

$jwtKeyBytes = New-Object byte[] 32
$jwtRng = [Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRng.GetBytes($jwtKeyBytes)
$jwtRng.Dispose()
$env:JWT_SIGNING_KEY = [Convert]::ToBase64String($jwtKeyBytes)
$env:JWT_SIGNING_KEY.Length