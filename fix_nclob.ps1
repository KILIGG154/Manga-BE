$ErrorActionPreference = "SilentlyContinue"
$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=localhost;Database=MangaSystemDB;Integrated Security=True;TrustServerCertificate=True;"
$conn.Open()

$tables = @{
    "[production_plan]" = @("Milestones", "Schedule", "ChapterTimeline", "Resources", "AssistantAllocation", "Risk", "pause_reason");
    "[chapter]" = @("recall_reason", "rejection_reason", "release_note");
    "[project]" = @("Description");
    "[sub_task]" = @("description");
    "[chapter_comment]" = @("Body");
    "[plan_comment]" = @("Body");
    "[submission_review]" = @("Comment");
    "[submission]" = @("Story", "CharacterDescription", "WorldSetting");
    "[feedbacks]" = @("content");
    "[development_plan]" = @("StoryDirection", "WorldSetting", "MainCharacters", "ArcPlanning", "Notes");
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
