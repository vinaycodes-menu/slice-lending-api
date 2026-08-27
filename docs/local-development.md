// to set DB_password for the enviroment variable in power shel

// $dbSecure = Read-Host -AsSecureString
// $dbCredential = [pscredential]::new("slice_app", $dbSecure)
// $env:DB_PASSWORD = $dbCredential.GetNetworkCredential().Password
//if ([string]::IsNullOrWhiteSpace($env:DB_PASSWORD)) {
//    "DB_PASSWORD is missing"
//} else {
//    "DB_PASSWORD is set"
//}