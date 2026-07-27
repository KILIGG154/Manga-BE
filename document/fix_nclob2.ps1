$ErrorActionPreference = "SilentlyContinue"
$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=localhost;Database=MangaSystemDB;Integrated Security=True;TrustServerCertificate=True;"
$conn.Open()

# Use exact case from schema query output
$tables = @{
    "production_plan" = @("chaptertimeline", "assistantallocation");
    "submission" = @("characterdescription", "worldsetting");
    "development_plan" = @("storydirection", "worldsetting", "maincharacters", "arcplanning");
}

foreach ($t in $tables.Keys) {
    foreach ($c in $tables[$t]) {
        $sql = "ALTER TABLE $t ALTER COLUMN ""$c"" NVARCHAR(MAX)"
        try {
            $cmd = $conn.CreateCommand()
            $cmd.CommandText = $sql
            $cmd.ExecuteNonQuery() | Out-Null
            Write-Host "OK: $t.$c"
        } catch {
            $msg = $_.Exception.Message.Substring(0, [Math]::Min(80, $_.Exception.Message.Length))
            Write-Host "ERR: $t.$c -> $msg"
        }
    }
}
$conn.Close()
Write-Host "DONE"
