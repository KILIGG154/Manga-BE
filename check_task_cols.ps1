$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=localhost;Database=MangaSystemDB;Integrated Security=True;TrustServerCertificate=True;"
$conn.Open()

$tables = @("task", "chapter")

foreach ($table in $tables) {
    Write-Host "=== $table ==="
    $sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '$table'"
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = $sql
    $rdr = $cmd.ExecuteReader()
    $idx = 0
    while ($rdr.Read()) {
        Write-Host "  [$idx] $($rdr[0]) : $($rdr[1]) [$($rdr[2])]"
        $idx++
    }
    $rdr.Close()
    Write-Host ""
}
$conn.Close()
