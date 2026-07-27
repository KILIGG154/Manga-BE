# UI Spec — Comment Thread (Decision Log §AI-05 + §AI-12)

> **Mục đích:** đặc tả UI cho FE khi tích hợp Comment vào Plan (khi PAUSED) và Chapter (sau Return/Recall).
> Style: thread giống GitHub / Linear — author + avatar + timestamp + body.
> **Không có edit/delete** — append-only (audit trail).

---

## 1. Khi nào UI hiện Comment

### 1.1 Plan Panel (khi Plan `PAUSED` — yêu cầu BA V3 §2.2)

- **Trigger**: mở Plan detail page.
- **Vị trí**: tab "Thảo luận" trong Plan panel (cạnh tab "Dashboard", "Chapters").
- **State**: nếu Plan đang PAUSED → badge đỏ trên tab "Thảo luận" với số comment mới `(3)`.

```
┌─────────────────────────────────────────────────────────┐
│ 📋 Plan: Arc Thanh Mẫu (PAUSED)        [Resume] [Pause] │
├─────────────────────────────────────────────────────────┤
│ [Dashboard] [Chapters] [Tasks] [Thảo luận (3) 🔴] [⚙]   │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Trang Nguyen  •  Tantou Editor  •  2h ago      │   │
│  │  Đề xuất: reschedule chapter 12 sang tuần sau  │   │
│  │  vì thiếu assistant.                            │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Linh Pham  •  Mangaka  •  1h ago                │   │
│  │  Em confirm có thể làm, cần 1 assistant nữa.    │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  [Viết comment... textarea]              [Gửi]          │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Chapter Panel (sau Return/Recall — Decision Log §AI-04)

- **Trigger**: Chapter ở `IN_PRODUCTION` (sau khi bị Return/Recall).
- **Vị trí**: tab "Thảo luận" trong Chapter detail (cạnh tab "Tasks", "Feedback").
- **Use case**: Tantou note lý do cụ thể, Team reply, Leader quyết.

```
┌─────────────────────────────────────────────────────────┐
│ 📖 Chapter 12 — Trang bố cục (IN_PRODUCTION)             │
│ Sau Return bởi Board: "Background chi tiết quá mức..."   │
├─────────────────────────────────────────────────────────┤
│ [Tasks] [Feedback] [Thảo luận (2)] [Lịch sử]            │
├─────────────────────────────────────────────────────────┤
│  ┌──────────────────────────────────────────────────┐   │
│  │  Trang Nguyen  •  Tantou  •  15m ago             │   │
│  │  Background trang 12 cần làm lại. Mangaka xem    │   │
│  │  qua nhé.                                        │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Linh Pham  •  Mangaka  •  5m ago                │   │
│  │  OK em sẽ sửa trong hôm nay.                     │   │
│  └──────────────────────────────────────────────────┘   │
│                                                          │
│  [Viết comment... textarea]              [Gửi]          │
└─────────────────────────────────────────────────────────┘
```

---

## 2. Component spec

### 2.1 `<CommentThread>` component

**Props**:
```typescript
interface CommentThreadProps {
  parentType: "PLAN" | "CHAPTER";
  parentId: number;
  currentUserId: number;
  currentUserName: string;
  currentUserRoles: string[];   // FE check hiển thị nút reply/mention
  wsChannel?: string;            // optional: WebSocket channel cho real-time
>
}
```

**State**:
```typescript
const [comments, setComments] = useState<CommentResponse[]>([]);
const [draft, setDraft] = useState<string>("");
const [submitting, setSubmitting] = useState<boolean>(false);
const [error, setError] = useState<string | null>(null);
```

**API call** (khi mount):
```typescript
const endpoint = parentType === "PLAN"
  ? `/api/workflow/plans/${parentId}/comments`
  : `/api/workflow/chapters/${parentId}/comments`;

fetch(endpoint, { headers: { Authorization: `Bearer ${token}` } })
  .then(res => res.json())
  .then(json => setComments(json.data));
```

**Submit handler**:
```typescript
async function handleSubmit() {
  if (draft.trim().length === 0) return;
  setSubmitting(true);
  try {
    const res = await fetch(endpoint, {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        authorId: currentUserId,
        body: draft.trim(),
      }),
    });
    if (!res.ok) {
      const err = await res.json();
      throw new Error(err.message);
    }
    const json = await res.json();
    setComments([...comments, json.data]);
    setDraft("");
  } catch (e: any) {
    setError(e.message);
  } finally {
    setSubmitting(false);
  }
}
```

### 2.2 `<CommentBubble>` component

**Props**:
```typescript
interface CommentBubbleProps {
  comment: CommentResponse;
  isOwnComment: boolean;
  highlight?: boolean;   // true nếu là comment mới trong 5 phút gần nhất
>
}
```

**Render**:
```tsx
<article className={cn("comment-bubble", { "bg-yellow-50": highlight, "ml-auto": isOwnComment })}>
  <header className="flex items-center gap-2 text-sm text-gray-600">
    <Avatar src={comment.authorAvatar} size={20} />
    <span className="font-medium">{comment.authorName}</span>
    <span>•</span>
    <time dateTime={comment.createdAt}>{formatRelativeTime(comment.createdAt)}</time>
  </header>
  <p className="mt-1 whitespace-pre-wrap">{comment.body}</p>
</article>
```

### 2.3 Phân quyền hiển thị

| Role | Action | UI |
|---|---|---|
| TANTOU_EDITOR | Post, View | ✅ đầy đủ |
| MANGAKA | Post, View | ✅ đầy đủ |
| ASSISTANT | Post, View | ✅ đầy đủ |
| EDITORIAL_BOARD_MEMBER | Post, View | ✅ đầy đủ |
| LEADER_BOARD | Post, View | ✅ đầy đủ |
| ADMIN | Post, View | ✅ đầy đủ |
| MANAGER | — | ❌ backend 403 (không có role) |
| Anonymous / chưa login | View only | FE hide nút "Gửi" |

**Không có edit/delete** — đơn giản hóa UI.

---

## 3. UX behaviors

### 3.1 Real-time update (optional — sprint 6+)

Nếu có WebSocket (đề xuất sprint 6+):
```typescript
useEffect(() => {
  if (!wsChannel) return;
  const socket = new WebSocket(`${WS_BASE}${wsChannel}`);
  socket.onmessage = (event) => {
    const newComment = JSON.parse(event.data);
    setComments(prev => [...prev, newComment]);
  };
  return () => socket.close();
}, [wsChannel]);
```

### 3.2 Optimistic UI

Khi user bấm "Gửi":
1. Tạo comment giả với `id: -1`, `createdAt: now`.
2. Append vào list ngay (highlight gradient → chuyển sang bình thường khi server trả về).
3. Nếu server lỗi → revert + hiển thị toast.

### 3.3 Empty state

Nếu `comments.length === 0`:
```
┌──────────────────────────────────────────┐
│  💬 Chưa có thảo luận nào.               │
│  Hãy là người đầu tiên bình luận!       │
└──────────────────────────────────────────┘
```

### 3.4 Validation / Error states

| Trường hợp | UI |
|---|---|
| Empty body | Disable nút "Gửi" |
| Body > 4000 ký tự | Hiển thị counter đỏ: `4001/4000` + disable submit |
| 401 (token hết hạn) | Toast "Phiên đăng nhập hết hạn, vui lòng login lại" |
| 403 (sai role) | Toast "Bạn không có quyền bình luận" |
| Network error | Toast "Lỗi mạng, vui lòng thử lại" + giữ draft |

---

## 4. Accessibility (A11y)

- Mỗi `<article>` có `aria-label` chứa author + timestamp.
- Textarea có `aria-label="Viết bình luận"` + `aria-describedby` cho counter.
- Nút "Gửi" có `aria-disabled` khi đang submit.
- Hỗ trợ keyboard: `Enter` để submit, `Shift+Enter` xuống dòng.

---

## 5. State diagram

```
[mở Plan/Chapter detail]
       │
       ▼
[load comments] ──→ [empty state]   ──→ [user bắt đầu nhập]
       │                                       │
       │                                       ▼
       │                                [submit POST]
       ▼                                       │
[render list] ◀──────── [success] ────── [optimistic UI]
       │                  │
       ▼                  ▼
[render list]    [error → revert + toast]
```

---

## 6. Sprint breakdown đề xuất

| Sprint | Task | Effort |
|---|---|:---:|
| 5 | Component `<CommentThread>` + `<CommentBubble>` cơ bản | 4 giờ |
| 5 | Hook vào Plan detail (khi PAUSED) | 2 giờ |
| 5 | Hook vào Chapter detail (khi IN_PRODUCTION) | 2 giờ |
| 5 | Empty state + validation + error handling | 2 giờ |
| 6 | WebSocket real-time update (optional) | 4 giờ |
| 6 | Mention @user + notification (optional) | 6 giờ |
| 6 | Markdown rendering (optional) | 3 giờ |

---

## 7. Vai trò của Comment trong Sprint 5

| Use case | Chỗ dùng | Comment giúp gì |
|---|---|---|
| Plan PAUSED — thiếu nhân sự | Plan panel | Team thảo luận giải pháp thay vì Zalo |
| Chapter bị Return | Chapter panel | Board/Tantou/Team thảo luận Task nào cần sửa |
| Chapter bị Recall | Chapter panel | Ghi nhận lý do + plan hành động |
| Plan milestone | Plan panel | Update tiến độ cho stakeholder |

**Lợi ích:**
- ✅ Audit trail — biết ai nói gì khi nào.
- ✅ Không mất context khi nhân viên nghỉ.
- ✅ Không cần rời hệ thống (Slack/Zalo).
- ✅ Giải quyết vấn đề "comment bị mất" — ghi chú Decision Log 2026-07-27 §AI-05.

---

## 8. Open questions cho FE

- [ ] Có hỗ trợ `@mention` user? (sprint 6+)
- [ ] Có render Markdown (bold, link) không? (sprint 6+)
- [ ] Có phân biệt comment "system" (vd: "Chapter recalled by Leader") với user comment? (sprint 6+)
- [ ] Có hiển thị ảnh/file đính kèm? (sprint 6+)
- [ ] Có infinite scroll hay pagination? (đề xuất: load all, pagination nếu > 100)