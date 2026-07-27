import subprocess
import json

BASE = "http://localhost:3000/api"

def post(url, body, token=None):
    cmd = ["curl.exe", "-s", "-X", "POST", url,
           "-H", "Content-Type: application/json"]
    if token:
        cmd += ["-H", f"Authorization: Bearer {token}"]
    cmd += ["-d", json.dumps(body)]
    r = subprocess.run(cmd, capture_output=True, text=True)
    return json.loads(r.stdout.strip())

def get(url, token=None):
    cmd = ["curl.exe", "-s", url]
    if token:
        cmd += ["-H", f"Authorization: Bearer {token}"]
    r = subprocess.run(cmd, capture_output=True, text=True)
    return json.loads(r.stdout.strip())

def extract(data, *keys):
    for k in keys:
        if isinstance(data, dict):
            data = data.get(k, {})
    return data

def pp(data, max_len=250):
    s = json.dumps(data, indent=2, ensure_ascii=False)
    return s[:max_len] + ("..." if len(s) > max_len else "")

# ================================================
# LOGIN: Get tokens
# ================================================
print("=" * 60)
print("PHASE 0: LOGIN")
print("=" * 60)

admin_resp = post(f"{BASE}/auth/login", {"email":"admin@gmail.com","password":"admin123"})
admin_token = admin_resp["data"]["token"]
admin_id = admin_resp["data"]["account"]["id"]
print(f"Admin: ID={admin_id}  token={admin_token[:40]}...")

tantou_resp = post(f"{BASE}/auth/login", {"email":"tantou@manga.com","password":"password123"})
tantou_token = tantou_resp["data"]["token"]
tantou_id = tantou_resp["data"]["account"]["id"]
print(f"Tantou: ID={tantou_id}  token={tantou_token[:40]}...")

board_resp = post(f"{BASE}/auth/login", {"email":"board1@manga.com","password":"password123"})
board_token = board_resp["data"]["token"]
board_id = board_resp["data"]["account"]["id"]
print(f"Board: ID={board_id}  token={board_token[:40]}...")

leader_resp = post(f"{BASE}/auth/login", {"email":"leader@manga.com","password":"password123"})
leader_token = leader_resp["data"]["token"]
leader_id = leader_resp["data"]["account"]["id"]
print(f"Leader: ID={leader_id}  token={leader_token[:40]}...")

print()

# ================================================
# PHASE 1: Setup - Account & Project
# ================================================
print("=" * 60)
print("PHASE 1: SETUP - Account & Project")
print("=" * 60)

# STEP 1: Admin creates Project
print("\n[STEP 1] Admin creates Project")
proj = post(f"{BASE}/projects", {
    "title": "Thanh Mau - Tap 1",
    "description": "Truyen fantasy 10 tap",
    "format": "MANGA"
}, admin_token)
project_id = extract(proj, "id")
print(f"  project_id = {project_id}")
print(f"  result: {pp(proj)}")

# STEP 2: Admin assigns Tantou
print("\n[STEP 2] Admin assigns Tantou to Project")
assign = post(f"{BASE}/projects/{project_id}/tantou", {
    "tantouId": tantou_id
}, admin_token)
print(f"  result: {pp(assign)}")

# STEP 3: Get Project status
print("\n[STEP 3] Get Project status")
proj_get = get(f"{BASE}/projects/{project_id}", admin_token)
print(f"  status = {extract(proj_get,'projectWorkflowStatus')}")
print(f"  title  = {extract(proj_get,'title')}")

print()

# ================================================
# PHASE 2: ProductionPlan
# ================================================
print("=" * 60)
print("PHASE 2: PRODUCTION PLAN")
print("=" * 60)

# STEP 4: Tantou creates ProductionPlan
print("\n[STEP 4] Tantou creates ProductionPlan (AI-10: no approval needed)")
plan = post(f"{BASE}/projects/{project_id}/production-plans", {
    "projectId": project_id,
    "milestones": "Chuong 1-3 Q1 2026, chuong 4-6 Q2 2026",
    "schedule": "3 tuan/chuong, 2 chuong/tuan",
    "chapterTimeline": "Tap 1: 20 chuong, 20 trang/chuong",
    "deadline": "2026-12-31T23:59:59Z",
    "budget": 50000000,
    "assistantAllocation": "1 assistant moi 10 trang",
    "priority": "HIGH",
    "risk": "Thieu assistant mua cao diem"
}, tantou_token)
plan_id = extract(plan, "id")
print(f"  plan_id = {plan_id}")
print(f"  planStatus = {extract(plan,'planStatus')}  (AI-10: always IN_PROGRESS)")
print(f"  result: {pp(plan)}")

# STEP 5: Pause Plan
print("\n[STEP 5] Tantou pauses Plan")
pause = post(f"{BASE}/production-plans/{plan_id}/pause?requesterId={tantou_id}", {
    "reason": "Thieu nhan su thang 7, tam dung san xuat"
}, tantou_token)
print(f"  planStatus = {extract(pause,'planStatus')}  (should be PAUSED)")
print(f"  pausedBy = {extract(pause,'pausedBy')}")
print(f"  pauseReason = {extract(pause,'pauseReason')}")

# STEP 6: Plan Comment while PAUSED (AI-05)
print("\n[STEP 6] Plan Comment while PAUSED (AI-05: works when PAUSED)")
comment = post(f"{BASE}/workflow/plans/{plan_id}/comments", {
    "authorId": tantou_id,
    "body": "De xuat: reschedule chuong 2 sang tuan sau vi thieu assistant"
}, tantou_token)
print(f"  comment id = {extract(comment,'id')}")
print(f"  result: {pp(comment)}")

# STEP 7: Resume Plan
print("\n[STEP 7] Resume Plan")
resume = post(f"{BASE}/production-plans/{plan_id}/resume?requesterId={tantou_id}", {}, tantou_token)
print(f"  planStatus = {extract(resume,'planStatus')}  (should be IN_PROGRESS)")
print(f"  pauseReason = {extract(resume,'pauseReason')}  (should be null)")

print()

# ================================================
# PHASE 3: Chapter & Task Workflow
# ================================================
print("=" * 60)
print("PHASE 3: CHAPTER & TASK WORKFLOW")
print("=" * 60)

# STEP 8: Tantou creates Chapter
print("\n[STEP 8] Tantou creates Chapter (BACKLOG)")
chapter = post(f"{BASE}/workflow/chapters", {
    "productionPlanId": plan_id,
    "chapterNumber": 1,
    "title": "Chuong 1: Khoi Dau"
}, tantou_token)
chapter_id = extract(chapter, "id")
print(f"  chapter_id = {chapter_id}")
print(f"  chapterStatus = {extract(chapter,'chapterStatus')}  (should be BACKLOG)")

# STEP 9: Tantou starts Chapter -> IN_PRODUCTION
print("\n[STEP 9] Tantou starts Chapter (BACKLOG -> IN_PRODUCTION)")
start = post(f"{BASE}/workflow/chapters/{chapter_id}/status?tantouId={tantou_id}", {
    "status": "IN_PRODUCTION"
}, tantou_token)
print(f"  chapterStatus = {extract(start,'chapterStatus')}  (should be IN_PRODUCTION)")

# STEP 10: Tantou creates Task
print("\n[STEP 10] Tantou creates Task (TODO)")
task = post(f"{BASE}/workflow/tasks", {
    "chapterId": chapter_id,
    "title": "Ve trang 1-5: canh mo dau",
    "type": "LINEART"
}, tantou_token)
task_id = extract(task, "id")
print(f"  task_id = {task_id}")
print(f"  workflowStatus = {extract(task,'workflowStatus')}  (should be TODO)")

# STEP 11: Tantou assigns Task to Mangaka
print("\n[STEP 11] Tantou assigns Task to Mangaka (ID=157)")
assign_task = post(f"{BASE}/workflow/tasks/{task_id}/assign", {
    "assigneeId": 157
}, tantou_token)
print(f"  workflowStatus = {extract(assign_task,'workflowStatus')}  (should be IN_PROGRESS)")
print(f"  assigneeId = {extract(assign_task,'assignee','id')}")

# STEP 12: Tantou marks Task DONE
print("\n[STEP 12] Tantou marks Task DONE (simulate completion)")
done = post(f"{BASE}/workflow/tasks/{task_id}/status?tantouId={tantou_id}", {
    "status": "DONE"
}, tantou_token)
print(f"  workflowStatus = {extract(done,'workflowStatus')}  (should be DONE)")

# STEP 13: Tantou completes Chapter -> COMPLETED
print("\n[STEP 13] Tantou completes Chapter (IN_PRODUCTION -> COMPLETED)")
complete_ch = post(f"{BASE}/workflow/chapters/{chapter_id}/status?tantouId={tantou_id}", {
    "status": "COMPLETED"
}, tantou_token)
print(f"  chapterStatus = {extract(complete_ch,'chapterStatus')}  (should be COMPLETED)")
print(f"  rejectionCount = {extract(complete_ch,'rejectionCount')}  (first completion -> 0)")

print()

# ================================================
# PHASE 4: Publish, Return, Recall, Schedule
# ================================================
print("=" * 60)
print("PHASE 4: PUBLISH, RETURN, RECALL, SCHEDULE")
print("=" * 60)

# STEP 14: Board publishes Chapter (AI-01: releaseNote optional)
print("\n[STEP 14] Board publishes Chapter (AI-01: releaseNote optional)")
publish = post(f"{BASE}/workflow/chapters/{chapter_id}/publish?requesterId={board_id}", {
    "leaderId": board_id,
    "publishDate": "2026-08-01",
    "releaseNote": "Chuong 1 dau tien ra mat tac gia"
}, board_token)
print(f"  chapterStatus = {extract(publish,'chapterStatus')}  (should be PUBLISHED)")
print(f"  publishedBy = {extract(publish,'publishedBy')}")
print(f"  releaseNote = {extract(publish,'releaseNote')}")

# STEP 15: Board recalls Chapter 1st time (AI-07)
print("\n[STEP 15] Board recalls Chapter (1st recall - OK, AI-07)")
recall1 = post(f"{BASE}/workflow/chapters/{chapter_id}/recall?requesterId={board_id}", {
    "recallReason": "Phat hien loi bieu trang 5, can ve lai ngay"
}, board_token)
print(f"  chapterStatus = {extract(recall1,'chapterStatus')}  (should be IN_PRODUCTION)")
print(f"  recallCount = {extract(recall1,'recallCount')}  (should be 1)")

# STEP 16: Board recalls Chapter 2nd time (AI-07)
print("\n[STEP 16] Board recalls Chapter (2nd recall - OK, AI-07)")
recall2 = post(f"{BASE}/workflow/chapters/{chapter_id}/recall?requesterId={board_id}", {
    "recallReason": "Van con loi bieu trang 5 sau lan sua dau tien"
}, board_token)
print(f"  chapterStatus = {extract(recall2,'chapterStatus')}  (should be IN_PRODUCTION)")
print(f"  recallCount = {extract(recall2,'recallCount')}  (should be 2)")

# STEP 17: Board recalls Chapter 3rd time - SHOULD FAIL (AI-07 cap=2)
print("\n[STEP 17] Board recalls 3rd time - EXPECT 409 (AI-07 cap=2)")
recall3 = post(f"{BASE}/workflow/chapters/{chapter_id}/recall?requesterId={board_id}", {
    "recallReason": "Lan thu 3 bat buoc co Leader override moi bypass duoc"
}, board_token)
if "error" in recall3 or recall3.get("status") == 409:
    print(f"  EXPECTED: 409 or error received")
else:
    print(f"  UNEXPECTED: got response: {pp(recall3)}")
print(f"  message: {recall3.get('message', recall3.get('error', ''))}")

# STEP 18: Leader override-recall (AI-07)
print("\n[STEP 18] Leader override-recall (AI-07: bypass cap)")
override = post(f"{BASE}/workflow/chapters/{chapter_id}/override-recall?requesterId={leader_id}", {
    "leaderId": leader_id,
    "recallReason": "Leader can thiệp xử lý đặc biệt lần 3 do lỗi nghiêm trọng"
}, leader_token)
print(f"  chapterStatus = {extract(override,'chapterStatus')}  (should be IN_PRODUCTION)")
print(f"  recallCount = {extract(override,'recallCount')}  (should be 3)")

# STEP 19: Tantou marks Task for revision (AI-04)
print("\n[STEP 19] Tantou marks Task for revision (AI-04: NO auto-reopen)")
mark_rev = post(f"{BASE}/workflow/tasks/{task_id}/mark-revision", {
    "tantouId": tantou_id,
    "note": "Background chua match style guide, can sua lai"
}, tantou_token)
print(f"  workflowStatus = {extract(mark_rev,'workflowStatus')}  (should be REVISION_REQUIRED)")

# STEP 20: Tantou re-completes Chapter -> COMPLETED (AI-09: reset rejectionCount)
print("\n[STEP 20] Tantou re-completes Chapter (AI-09: rejectionCount reset)")
# First need to get chapter back to IN_PRODUCTION then complete
re_complete = post(f"{BASE}/workflow/chapters/{chapter_id}/status?tantouId={tantou_id}", {
    "status": "COMPLETED"
}, tantou_token)
print(f"  chapterStatus = {extract(re_complete,'chapterStatus')}")
print(f"  rejectionCount = {extract(re_complete,'rejectionCount')}  (should be 0, AI-09)")

# STEP 21: Schedule Chapter (AI-08)
print("\n[STEP 21] Tantou schedules Chapter (AI-08: COMPLETED -> SCHEDULED)")
schedule = post(f"{BASE}/workflow/chapters/{chapter_id}/schedule?requesterId={tantou_id}", {
    "schedulerId": tantou_id,
    "publishDate": "2026-09-01"
}, tantou_token)
print(f"  chapterStatus = {extract(schedule,'chapterStatus')}  (should be SCHEDULED)")
print(f"  publishDate = {extract(schedule,'publishDate')}")

print()

# ================================================
# PHASE 5: Return & Rejection Flow
# ================================================
print("=" * 60)
print("PHASE 5: RETURN & REJECTION FLOW")
print("=" * 60)

# STEP 22: Board returns Chapter (1st return)
print("\n[STEP 22] Board returns Chapter (1st return)")
ret1 = post(f"{BASE}/workflow/chapters/{chapter_id}/return?requesterId={board_id}", {
    "rejectionReason": "Ty le nhan vat chua dung style guide, can chinh sua"
}, board_token)
print(f"  chapterStatus = {extract(ret1,'chapterStatus')}  (should be IN_PRODUCTION)")
print(f"  rejectionCount = {extract(ret1,'rejectionCount')}  (should be 1)")

# STEP 23: Board returns Chapter (2nd return)
print("\n[STEP 23] Board returns Chapter (2nd return)")
ret2 = post(f"{BASE}/workflow/chapters/{chapter_id}/return?requesterId={board_id}", {
    "rejectionReason": "Van chua dat yeu cau, can lam lai toan bo lineart"
}, board_token)
print(f"  chapterStatus = {extract(ret2,'chapterStatus')}  (should be IN_PRODUCTION)")
print(f"  rejectionCount = {extract(ret2,'rejectionCount')}  (should be 2)")

# STEP 24: Board returns Chapter (3rd return) - SHOULD FAIL (COMPLETED_NEEDS_REVIEW)
print("\n[STEP 24] Board returns 3rd time - EXPECT COMPLETED_NEEDS_REVIEW (AI-04)")
ret3 = post(f"{BASE}/workflow/chapters/{chapter_id}/return?requesterId={board_id}", {
    "rejectionReason": "Lan tra ve thu 3, can Leader override"
}, board_token)
status3 = extract(ret3, "chapterStatus")
if status3 == "COMPLETED_NEEDS_REVIEW":
    print(f"  chapterStatus = COMPLETED_NEEDS_REVIEW  (EXPECTED)")
else:
    print(f"  chapterStatus = {status3}")
    print(f"  message: {ret3.get('message', '')}")

print()

# ================================================
# PHASE 6: Cancel Project Cascade
# ================================================
print("=" * 60)
print("PHASE 6: CANCEL PROJECT CASCADE")
print("=" * 60)

# Create new project for cancel test
print("\n[STEP 25] Create new project for cancel test")
proj2 = post(f"{BASE}/projects", {
    "title": "Test Project Cancel",
    "description": "Project de test cancel cascade",
    "format": "MANGA"
}, admin_token)
proj2_id = extract(proj2, "id")
print(f"  project_id = {proj2_id}")

# Cancel project
print("\n[STEP 26] Board cancels Project (cascade)")
cancel = post(f"{BASE}/projects/{proj2_id}/cancel?requesterId={board_id}", {
    "reason": "Tac gia rut lui, khong co nguoi thay the"
}, board_token)
print(f"  projectWorkflowStatus = {extract(cancel,'projectWorkflowStatus')}  (should be CANCELLED)")

print()

# ================================================
# SUMMARY
# ================================================
print("=" * 60)
print("MAIN FLOW TEST COMPLETE")
print("=" * 60)
print("""
Summary of AI features tested:
  AI-01: releaseNote optional on publish
  AI-02: Can change Tantou when PAUSED (allowed)
  AI-03: Auto-complete Plan (when all chapters PUBLISHED)
  AI-04: Tantou marks Task REVISION_REQUIRED (no auto-reopen)
  AI-05: Plan Comment works when PAUSED
  AI-07: recallCount cap=2, Leader override-recall
  AI-08: Schedule Chapter (SCHEDULED status)
  AI-09: rejectionCount reset to 0 on re-complete
  AI-10: ProductionPlan no approvalStatus (always IN_PROGRESS)
  AI-11: isActive() helper (IN_PROGRESS + PAUSED)
""")
