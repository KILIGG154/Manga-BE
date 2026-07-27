#!/bin/bash
# =============================================================================
# Quick-Start Script — Manga System Main Flow
# =============================================================================
# Usage:  bash quickstart.sh
# Prereq: Server running on port 6003 (default)
#         If server is on different port, set: export BASE_URL=http://localhost:XXXX/api
# =============================================================================

BASE_URL="${BASE_URL:-http://localhost:6003/api}"
AUTH="Content-Type: application/json"

echo "=============================================="
echo "  MANGA SYSTEM — Quick-Start Main Flow"
echo "=============================================="
echo "  Base URL: $BASE_URL"
echo ""

# --- LOGIN ---
echo "[0] Login..."

ADMIN_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "$AUTH" \
  -d '{"email":"admin@gmail.com","password":"admin123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

if [ -z "$ADMIN_TOKEN" ]; then
  echo "FATAL: Could not login as admin"
  exit 1
fi
echo "  Admin token: ${ADMIN_TOKEN:0:30}..."

TANTOU_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "$AUTH" \
  -d '{"email":"tantou@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

BOARD_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "$AUTH" \
  -d '{"email":"board1@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

LEADER_TOKEN=$(curl -s -X POST "$BASE_URL/auth/login" \
  -H "$AUTH" \
  -d '{"email":"leader@manga.com","password":"password123"}' \
  | grep -o '"token":"[^"]*"' | sed 's/"token":"\(.*\)"/\1/')

# --- PHASE 1: Create Project ---
echo ""
echo "[1] Create project..."
PROJECT_RESP=$(curl -s -X POST "$BASE_URL/projects" \
  -H "$AUTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"QuickStart Manga","description":"Test manga project","format":"MANGA"}')
PROJECT_ID=$(echo $PROJECT_RESP | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  Project ID: $PROJECT_ID"

# Activate project
curl -s -X PUT "$BASE_URL/projects/$PROJECT_ID/status" \
  -H "$AUTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status":"ACTIVE"}' > /dev/null
echo "  Project: ACTIVE"

# --- PHASE 2: Create ProductionPlan ---
echo ""
echo "[2] Create ProductionPlan..."
PLAN_RESP=$(curl -s -X POST "$BASE_URL/projects/$PROJECT_ID/production-plans" \
  -H "$AUTH" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"milestones":"Chuong 1-3","schedule":"3 tuan/chuong","deadline":"2026-12-31T23:59:59Z","budget":50000000,"priority":"HIGH"}')
PLAN_ID=$(echo $PLAN_RESP | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
PLAN_STATUS=$(echo $PLAN_RESP | grep -o '"planStatus":"[^"]*"' | sed 's/"planStatus":"\(.*\)"/\1/')
echo "  Plan ID: $PLAN_ID, Status: $PLAN_STATUS"

# --- PHASE 3: Create Chapter ---
echo ""
echo "[3] Create Chapter..."
CHAPTER_RESP=$(curl -s -X POST "$BASE_URL/workflow/chapters?requesterId=156" \
  -H "$AUTH" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d "{\"planId\":$PLAN_ID,\"chapterNumber\":1,\"title\":\"Chuong 1\"}")
CHAPTER_ID=$(echo $CHAPTER_RESP | grep -o '"id":[0-9]*' | head -1 | sed 's/"id"://')
echo "  Chapter ID: $CHAPTER_ID"

# --- PHASE 4: Update Chapter Status IN_PRODUCTION ---
echo ""
echo "[4] Update Chapter to IN_PRODUCTION..."
# NOTE: Due to NCLOB conversion issue in local DB, this may fail with 500 error
CHAPTER_UPD=$(curl -s -X PUT "$BASE_URL/workflow/chapters/$CHAPTER_ID/status?requesterId=156&status=IN_PRODUCTION" \
  -H "$AUTH" \
  -H "Authorization: Bearer $TANTOU_TOKEN" \
  -d '{}')
CHAPTER_STATUS=$(echo $CHAPTER_UPD | grep -o '"chapterStatus":"[^"]*"' | sed 's/"chapterStatus":"\(.*\)"/\1/')
if [ -z "$CHAPTER_STATUS" ]; then
  echo "  (NCLOB issue — see DB migration docs)"
else
  echo "  Chapter Status: $CHAPTER_STATUS"
fi

# --- PHASE 5: Publish Chapter (as Board) ---
echo ""
echo "[5] Board publishes Chapter..."
PUBLISH_RESP=$(curl -s -X POST "$BASE_URL/workflow/chapters/$CHAPTER_ID/publish?requesterId=153" \
  -H "$AUTH" \
  -H "Authorization: Bearer $BOARD_TOKEN" \
  -d '{"leaderId":153,"publishDate":"2026-08-01","releaseNote":"Test release"}')
echo "  Publish Response: ${PUBLISH_RESP:0:200}"

# --- PHASE 6: Recall Chapter ---
echo ""
echo "[6] Board recalls Chapter..."
RECALL_RESP=$(curl -s -X POST "$BASE_URL/workflow/chapters/$CHAPTER_ID/recall?requesterId=153" \
  -H "$AUTH" \
  -H "Authorization: Bearer $BOARD_TOKEN" \
  -d '{"recallReason":"Thu hoi de sua chinh sua bieu trang"}')
echo "  Recall Response: ${RECALL_RESP:0:200}"

# --- PHASE 7: Return Chapter ---
echo ""
echo "[7] Board returns Chapter to production..."
RETURN_RESP=$(curl -s -X POST "$BASE_URL/workflow/chapters/$CHAPTER_ID/return?requesterId=153" \
  -H "$AUTH" \
  -H "Authorization: Bearer $BOARD_TOKEN" \
  -d '{"rejectionReason":"Ty le chua dung style guide"}')
echo "  Return Response: ${RETURN_RESP:0:200}"

echo ""
echo "=============================================="
echo "  Quick-Start Complete!"
echo "=============================================="
echo ""
echo "NOTES:"
echo "  - Phase 4 may fail with HTTP 500 if NCLOB migration not applied"
echo "  - Run DB migration: powershell -ExecutionPolicy Bypass -File fix_nclob.ps1"
echo "  - See API_USAGE.md §9 for full documentation"
