@echo off
echo ================================================
echo MANGA SYSTEM - MAIN FLOW TEST
echo ================================================
echo.

python - <<EOF
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

# Login all accounts
print("=== LOGIN: Admin, Tantou, Board ===")
admin_resp = post(f"{BASE}/auth/login", {"email":"admin@gmail.com","password":"admin123"})
admin_token = admin_resp["data"]["token"]
admin_id = admin_resp["data"]["account"]["id"]
print(f"Admin: ID={admin_id}, token={admin_token[:30]}...")

tantou_resp = post(f"{BASE}/auth/login", {"email":"tantou@manga.com","password":"password123"})
tantou_token = tantou_resp["data"]["token"]
tantou_id = tantou_resp["data"]["account"]["id"]
print(f"Tantou: ID={tantou_id}, token={tantou_token[:30]}...")

board_resp = post(f"{BASE}/auth/login", {"email":"board1@manga.com","password":"password123"})
board_token = board_resp["data"]["token"]
board_id = board_resp["data"]["account"]["id"]
print(f"Board: ID={board_id}, token={board_token[:30]}...")
print()

# STEP 1: Admin creates Project
print("=== STEP 1: Admin creates Project ===")
proj = post(f"{BASE}/projects", {
    "title": "Thanh Mau - Tap 1",
    "description": "Truyen fantasy 10 tap",
    "format": "MANGA"
}, admin_token)
project_id = extract(proj, "id")
print(f"Result: project_id={project_id}")
print(json.dumps(proj, indent=2, ensure_ascii=False)[:300])
print()

# STEP 2: Admin assigns Tantou
print("=== STEP 2: Admin assigns Tantou to Project ===")
assign = post(f"{BASE}/projects/{project_id}/tantou", {
    "tantouId": tantou_id
}, admin_token)
print(json.dumps(assign, indent=2, ensure_ascii=False)[:300])
print()

# STEP 3: Get Project status
print("=== STEP 3: Get Project status ===")
proj_get = get(f"{BASE}/projects/{project_id}", admin_token)
print(f"Project status: {extract(proj_get,'projectWorkflowStatus')}")
print(f"Project title: {extract(proj_get,'title')}")
print()

# STEP 4: Tantou creates ProductionPlan
print("=== STEP 4: Tantou creates ProductionPlan ===")
plan = post(f"{BASE}/projects/{project_id}/production-plans", {
    "projectId": project_id,
    "milestones": "Chuong 1-3 Q1 2026",
    "schedule": "3 tuan/chuong",
    "chapterTimeline": "Tap 1: 20 chuong, 20 trang/chuong",
    "deadline": "2026-12-31T23:59:59Z",
    "budget": 50000000,
    "priority": "HIGH"
}, tantou_token)
plan_id = extract(plan, "id")
print(f"Result: plan_id={plan_id}")
print(f"Plan status: {extract(plan,'planStatus')}")
print()

# STEP 5: Tantou creates Chapter
print("=== STEP 5: Tantou creates Chapter ===")
chapter = post(f"{BASE}/workflow/chapters", {
    "productionPlanId": plan_id,
    "chapterNumber": 1,
    "title": "Chuong 1: Khoi Dau"
}, tantou_token)
chapter_id = extract(chapter, "id")
print(f"Result: chapter_id={chapter_id}")
print(f"Chapter status: {extract(chapter,'chapterStatus')}")
print()

# STEP 6: Tantou start Chapter -> IN_PRODUCTION
print("=== STEP 6: Tantou start Chapter IN_PRODUCTION ===")
start = post(f"{BASE}/workflow/chapters/{chapter_id}/status", {
    "status": "IN_PRODUCTION"
}, tantou_token)
# Note: may need tantouId param
start2 = post(f"{BASE}/workflow/chapters/{chapter_id}/status?tantouId={tantou_id}", {
    "status": "IN_PRODUCTION"
}, tantou_token)
chapter_status = extract(start2, "chapterStatus")
print(f"Chapter status after start: {chapter_status}")
print()

# STEP 7: Tantou creates Task
print("=== STEP 7: Tantou creates Task ===")
task = post(f"{BASE}/workflow/tasks", {
    "chapterId": chapter_id,
    "title": "Ve trang 1-5: canh mo dau",
    "type": "LINEART"
}, tantou_token)
task_id = extract(task, "id")
print(f"Result: task_id={task_id}")
print(f"Task status: {extract(task,'workflowStatus')}")
print()

# STEP 8: Tantou assigns Task to Mangaka
print("=== STEP 8: Tantou assigns Task to Mangaka ===")
mangaka_id = 157  # from login
assign_task = post(f"{BASE}/workflow/tasks/{task_id}/assign", {
    "assigneeId": mangaka_id
}, tantou_token)
print(f"Assign result: {json.dumps(assign_task, indent=2, ensure_ascii=False)[:200]}")
print()

# STEP 9: Tantou marks Task DONE (simplified - skip SubTask/Submission for now)
print("=== STEP 9: Tantou marks Task DONE ===")
done_task = post(f"{BASE}/workflow/tasks/{task_id}/status?tantouId={tantou_id}", {
    "status": "DONE"
}, tantou_token)
print(f"Task status: {extract(done_task,'workflowStatus')}")
print()

# STEP 10: Tantou complete Chapter -> COMPLETED
print("=== STEP 10: Tantou complete Chapter COMPLETED ===")
complete_ch = post(f"{BASE}/workflow/chapters/{chapter_id}/status?tantouId={tantou_id}", {
    "status": "COMPLETED"
}, tantou_token)
chapter_status = extract(complete_ch, "chapterStatus")
print(f"Chapter status: {chapter_status}")
print()

# STEP 11: Board publishes Chapter (AI-01: releaseNote optional - NO body)
print("=== STEP 11: Board publishes Chapter (AI-01: releaseNote optional) ===")
publish = post(f"{BASE}/workflow/chapters/{chapter_id}/publish?requesterId={board_id}", {
    "leaderId": board_id,
    "publishDate": "2026-08-01"
}, board_token)
chapter_status = extract(publish, "chapterStatus")
print(f"Chapter status: {chapter_status}")
print(f"Published by: {extract(publish,'publishedBy')}")
print()

# STEP 12: Board recalls Chapter (AI-07)
print("=== STEP 12: Board recalls Chapter (1st recall - OK) ===")
recall1 = post(f"{BASE}/workflow/chapters/{chapter_id}/recall?requesterId={board_id}", {
    "recallReason": "Phat hien sai lam trang 5, can ve lai ngay"
}, board_token)
chapter_status = extract(recall1, "chapterStatus")
recall_count = extract(recall1, "recallCount")
print(f"Chapter status: {chapter_status}")
print(f"Recall count: {recall_count}")
print()

# STEP 13: Board recalls again (2nd time - OK)
print("=== STEP 13: Board recalls 2nd time (OK) ===")
recall2 = post(f"{BASE}/workflow/chapters/{chapter_id}/recall?requesterId={board_id}", {
    "recallReason": "Van con loi trang 5 sau lan sua dau tien"
}, board_token)
chapter_status = extract(recall2, "chapterStatus")
recall_count = extract(recall2, "recallCount")
print(f"Chapter status: {chapter_status}")
print(f"Recall count: {recall_count}")
print()

# STEP 14: Board recalls 3rd time - SHOULD FAIL (AI-07 cap = 2)
print("=== STEP 14: Board recalls 3rd time - EXPECT 409 (cap=2) ===")
try:
    recall3 = post(f"{BASE}/workflow/chapters/{chapter_id}/recall?requesterId={board_id}", {
        "recallReason": "Lan thu 3 - phai co Leader override de bypass"
    }, board_token)
    print(f"UNEXPECTED: got 200 response: {json.dumps(recall3, indent=2, ensure_ascii=False)[:200]}")
except Exception as e:
    print(f"Error: {e}")
print()

# STEP 15: Board returns Chapter (AI-04 + AI-09)
print("=== STEP 15: Board returns Chapter (AI-09: rejectionCount) ===")
ret = post(f"{BASE}/workflow/chapters/{chapter_id}/return?requesterId={board_id}", {
    "rejectionReason": "Ty le nhan vat chua dung style guide"
}, board_token)
chapter_status = extract(ret, "chapterStatus")
rejection_count = extract(ret, "rejectionCount")
print(f"Chapter status: {chapter_status}")
print(f"Rejection count: {rejection_count}")
print()

# STEP 16: Pause Plan (AI-02)
print("=== STEP 16: Pause Plan ===")
pause = post(f"{BASE}/production-plans/{plan_id}/pause?requesterId={tantou_id}", {
    "reason": "Thieu nhan su thang 7"
}, tantou_token)
plan_status = extract(pause, "planStatus")
print(f"Plan status: {plan_status}")
print()

# STEP 17: Plan Comment while PAUSED (AI-05)
print("=== STEP 17: Plan Comment while PAUSED (works when PAUSED) ===")
comment = post(f"{BASE}/workflow/plans/{plan_id}/comments", {
    "authorId": tantou_id,
    "body": "De xuat: reschedule chuong 2 sang tuan sau"
}, tantou_token)
print(f"Comment created: {json.dumps(comment, indent=2, ensure_ascii=False)[:200]}")
print()

# STEP 18: Resume Plan
print("=== STEP 18: Resume Plan ===")
resume = post(f"{BASE}/production-plans/{plan_id}/resume?requesterId={tantou_id}", {}, tantou_token)
plan_status = extract(resume, "planStatus")
print(f"Plan status: {plan_status}")
print()

# STEP 19: Force Close Plan
print("=== STEP 19: Force Close Plan ===")
force = post(f"{BASE}/production-plans/{plan_id}/force-close?requesterId={admin_id}", {
    "reason": "Du an het budget, ket thuc som"
}, admin_token)
plan_status = extract(force, "planStatus")
print(f"Plan status: {plan_status}")
print()

print("================================================")
print("MAIN FLOW TEST COMPLETE")
print("================================================")
EOF
