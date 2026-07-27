# SCRIPT FILES COLLECTION
**Ngày tạo:** 2026-07-27  
**Thư mục:** document/scripts

---

## 1. fix_nclob.ps1

PowerShell script để alter database columns từ ntext/text sang nvarchar(max).

```powershell
# PowerShell - Fix NCLOB columns
$ErrorActionPreference = "SilentlyContinue"
$conn = New-Object System.Data.SqlClient.SqlConnection
$conn.ConnectionString = "Server=localhost;Database=MangaSystemDB;Integrated Security=True;TrustServerCertificate=True;"
$conn.Open()

# Tables and columns to alter
$tables = @{
    "production_plan" = @("chaptertimeline", "assistantallocation");
    "submission" = @("characterdescription", "worldsetting");
    "development_plan" = @("storydirection", "worldsetting", "maincharacters", "arcplanning");
}

foreach ($t in $tables.Keys) {
    foreach ($c in $tables[$t]) {
        $sql = "ALTER TABLE $t ALTER COLUMN `"$c`" NVARCHAR(MAX)"
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
```

**Cách chạy:**
```powershell
powershell -ExecutionPolicy Bypass -File fix_nclob.ps1
```

---

## 2. test_flow.js

Node.js script để test end-to-end flow.

**Cách chạy:**
```bash
# Set port và chạy
$env:SERVER_PORT="6003"
node test_flow.js
```

**Test coverage:**
- Phase 1: Login (Admin, Tantou, Board, Leader)
- Phase 2: Project Setup
- Phase 3: Chapter Workflow
- Phase 4: Publish/Recall/Override
- Phase 5: Return & Rejection
- Phase 6: Cancel Project

**Current Issues:**
- Phase 3b: 500 NCLOB error
- Phase 3c-3d: 400 validation fail
- Phase 4+: 409 conflict (do Phase 3 fail)

---

## 3. SQL Migration Script

**File:** `V2026_07_27__alter_text_columns_to_nvarcharmax.sql`

```sql
-- Migration: Convert text columns to nvarchar(max)
-- Run with Flyway or manually via SSMS

-- Chapter table
ALTER TABLE chapter ALTER COLUMN description NVARCHAR(MAX);
ALTER TABLE chapter ALTER COLUMN storyboardnotes NVARCHAR(MAX);

-- Submission table
ALTER TABLE submission ALTER COLUMN content NVARCHAR(MAX);
ALTER TABLE submission ALTER COLUMN characterdescription NVARCHAR(MAX);
ALTER TABLE submission ALTER COLUMN worldsetting NVARCHAR(MAX);
ALTER TABLE submission ALTER COLUMN reviewerfeedback NVARCHAR(MAX);

-- SubTask table
ALTER TABLE subtask ALTER COLUMN description NVARCHAR(MAX);
ALTER TABLE subtask ALTER COLUMN feedback NVARCHAR(MAX);
ALTER TABLE subtask ALTER COLUMN notes NVARCHAR(MAX);

-- Task table
ALTER TABLE task ALTER COLUMN description NVARCHAR(MAX);
ALTER TABLE task ALTER COLUMN notes NVARCHAR(MAX);

-- Project table
ALTER TABLE project ALTER COLUMN description NVARCHAR(MAX);

-- ProductionPlan table
ALTER TABLE production_plan ALTER COLUMN chaptertimeline NVARCHAR(MAX);
ALTER TABLE production_plan ALTER COLUMN assistantallocation NVARCHAR(MAX);

-- DevelopmentPlan table
ALTER TABLE development_plan ALTER COLUMN storydirection NVARCHAR(MAX);
ALTER TABLE development_plan ALTER COLUMN worldsetting NVARCHAR(MAX);
ALTER TABLE development_plan ALTER COLUMN maincharacters NVARCHAR(MAX);
ALTER TABLE development_plan ALTER COLUMN arcplanning NVARCHAR(MAX);

-- Feedback table
ALTER TABLE feedback ALTER COLUMN content NVARCHAR(MAX);

-- Vote table
ALTER TABLE vote ALTER COLUMN comment NVARCHAR(MAX);
```

---

## 4. Entity Annotation Fixes

Các entity đã được fix `@Lob` → `@Column(columnDefinition = "nvarchar(max)")`:

### Chapter.java
```java
// BEFORE
@Lob
@Column(name = "description")
private String description;

// AFTER
@Column(name = "description", columnDefinition = "nvarchar(max)")
private String description;
```

### Submission.java
```java
// Fields fixed:
private String content;
private String reviewerFeedback;
```

### SubTask.java
```java
// Fields fixed:
private String description;
private String feedback;
private String notes;
```

### Task.java
```java
// Fields fixed:
private String description;
private String notes;
```

---

## 5. Server Startup Commands

```powershell
# Kill port
netstat -ano | findstr :8080
taskkill /PID <pid> /F

# Start server
./mvnw spring-boot:run -DskipTests

# Or with specific port
$env:SERVER_PORT="6003"
./mvnw spring-boot:run -DskipTests
```

---

**Document created:** 2026-07-27  
**Status:** Ready to use
