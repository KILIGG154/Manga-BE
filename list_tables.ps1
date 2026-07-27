$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=localhost;Database=MangaSystemDB;Integrated Security=True;TrustServerCertificate=True;"
$conn.Open()
$cmd = $conn.CreateCommand()
$cmd.CommandText = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'"
$rdr = $cmd.ExecuteReader()
while ($rdr.Read()) {
    Write-Host $rdr[0]
}
$conn.Close()
