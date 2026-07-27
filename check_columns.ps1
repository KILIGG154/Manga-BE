$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=localhost;Database=MangaSystemDB;Integrated Security=True;TrustServerCertificate=True;"
$conn.Open()

$columns = @(
    "production_plan.Milestones",
    "production_plan.Schedule",
    "production_plan.ChapterTimeline",
    "production_plan.Resources",
    "production_plan.AssistantAllocation",
    "production_plan.Risk",
    "production_plan.pause_reason",
    "chapter.recall_reason",
    "chapter.rejection_reason",
    "chapter.release_note",
    "project.Description",
    "sub_task.description",
    "chapter_comment.Body",
    "plan_comment.Body",
    "submission_review.Comment",
    "submission.Story",
    "submission.CharacterDescription",
    "submission.WorldSetting",
    "feedbacks.content",
    "development_plan.StoryDirection",
    "development_plan.WorldSetting",
    "development_plan.MainCharacters",
    "development_plan.ArcPlanning",
    "development_plan.Notes"
)

foreach ($col in $columns) {
    $parts = $col.Split(".")
    $table = $parts[0]
    $column = $parts[1]
    $sql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '$table' AND COLUMN_NAME = '$column'"
    $cmd = $conn.CreateCommand()
    $cmd.CommandText = $sql
    $rdr = $cmd.ExecuteReader()
    if ($rdr.Read()) {
        Write-Host "$table.$column : $($rdr[0]) $($rdr[1]) [$($rdr[2])]"
    } else {
        Write-Host "$table.$column : NOT FOUND"
    }
    $rdr.Close()
}
$conn.Close()
