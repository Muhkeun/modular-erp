# UX Standards for SaaS ERP

> 상용 SaaS ERP (SAP Fiori, Microsoft Dynamics, NetSuite) 수준의 UX를 목표로 합니다.
> 기능이 아닌 사용자 경험 관점에서의 갭과 기준을 정의합니다.

## Core UX Principles

1. **Task-based, not menu-based** - "오늘 처리할 일" 중심. 메뉴 계층 탐색 최소화
2. **Role-based** - 구매담당자, 창고관리자, 경영진은 각각 다른 홈을 봄
3. **Consistent patterns** - 모든 리스트/상세/생성 화면이 동일한 패턴
4. **Keyboard-first** - ERP 사용자는 마우스보다 키보드가 빠름
5. **Progressive disclosure** - 기본은 단순, 고급은 펼쳐서

## Current UX Gaps

### GAP-U01: 역할 기반 홈/대시보드
- **현재**: 단일 DashboardPage (모든 역할 동일)
- **목표**: 역할별 위젯/KPI/작업 인박스
  - 구매: 미결 PR, 승인 대기 PO, 납기 임박 GR
  - 재고: 재고 부족 알림, 입고 예정, 실사 스케줄
  - 재무: 미결 전표, 마감 진행률, 예산 소진율
  - 경영진: 매출/매입 추이, 현금흐름, 주요 KPI

### GAP-U02: Saved View / Filter Preset
- **현재**: 리스트 화면에 필터 있으나 저장/공유 불가
- **목표**: 사용자/팀/전사 단위 뷰 저장, 즐겨찾기, 기본 뷰 설정
- **참고**: AG Grid 상태 저장은 useGridPreference로 구현됨 (연결만 하면 됨)

### GAP-U03: Bulk Action / Inline Edit
- **현재**: 단건 상세 페이지에서만 편집
- **목표**: 리스트에서 체크박스 선택 → 일괄 승인/반려/삭제/상태변경
- **목표**: 인라인 셀 편집 (AG Grid 기반)

### GAP-U04: 모바일/태블릿
- **현재**: 반응형 미완 (데스크톱 우선)
- **목표**: 창고 UX (바코드 스캔, 입고확인), 결재 UX (모바일 승인)

### GAP-U05: 알림 ↔ 업무 연결
- **현재**: 알림 목록만 표시
- **목표**: 알림 클릭 → 해당 문서로 직접 이동 + 인라인 액션 (승인/반려)

### GAP-U06: 워크플로우 디자이너
- **현재**: 워크플로우 조회/활성화만 가능
- **목표**: 드래그&드롭 워크플로우 편집기 (조건분기, 병렬승인, 위임규칙)

---

## Standard Page Patterns

모든 비즈니스 모듈 페이지는 아래 패턴을 따릅니다:

### List Page
```
┌─────────────────────────────────────────┐
│ [Page Title]              [+ New] [Export]│
│ ┌─ Filter Bar ─────────────────────────┐ │
│ │ [Status ▾] [Date Range] [Search...] │ │
│ │ [Saved Views ▾]        [Clear][Apply]│ │
│ └──────────────────────────────────────┘ │
│ ┌─ AG Grid ────────────────────────────┐ │
│ │ ☐ │ No │ Date │ Status │ Amount │... │ │
│ │ ☐ │ ...│ ...  │ ...    │ ...    │... │ │
│ └──────────────────────────────────────┘ │
│ [Selected: 3] [Bulk Approve] [Bulk Delete]│
│ Showing 1-50 of 234     [< 1 2 3 4 5 >] │
└─────────────────────────────────────────┘
```

### Detail Page
```
┌─────────────────────────────────────────┐
│ [← Back] PR-2026-0042        [Draft ●] │
│ ┌─ Header ─────────────────────────────┐│
│ │ Requester: ...  Date: ...  Dept: ... ││
│ └──────────────────────────────────────┘│
│ ┌─ Tabs ───────────────────────────────┐│
│ │ [Items] [Approvals] [History] [Files]││
│ └──────────────────────────────────────┘│
│ ┌─ Content ────────────────────────────┐│
│ │ (탭별 내용)                           ││
│ └──────────────────────────────────────┘│
│ [Edit] [Submit for Approval] [Print]    │
└─────────────────────────────────────────┘
```

### Create/Edit Page
```
┌─────────────────────────────────────────┐
│ [← Cancel] New Purchase Request         │
│ ┌─ Form ───────────────────────────────┐│
│ │ Requester: [________]                ││
│ │ Department: [▾ Select]               ││
│ │ Required Date: [📅 ________]         ││
│ └──────────────────────────────────────┘│
│ ┌─ Line Items ─────────────────────────┐│
│ │ (Editable AG Grid)                   ││
│ │ [+ Add Row]                          ││
│ └──────────────────────────────────────┘│
│            [Save Draft] [Submit]        │
└─────────────────────────────────────────┘
```

---

## Keyboard Shortcuts (확장 계획)

| 단축키 | 현재 | 목표 |
|--------|------|------|
| Ctrl/Cmd + K | ✅ Command Palette | 유지 |
| Ctrl/Cmd + N | ✅ New Record | 유지 |
| Ctrl/Cmd + S | ✅ Save | 유지 |
| F5 | ✅ Refresh | 유지 |
| Ctrl/Cmd + Enter | ❌ | Submit/Approve |
| Ctrl/Cmd + F | ❌ | 리스트 내 검색 포커스 |
| Esc | ❌ | 모달 닫기/편집 취소 |
| Tab/Shift+Tab | ❌ | 폼 필드 이동 |
| ↑/↓ | ❌ | 리스트 행 이동 |
| Enter | ❌ | 선택한 행 상세 열기 |

---

## Accessibility (접근성)

상용 SaaS 기준 최소 요구사항:

- [ ] WCAG 2.1 AA 준수
- [ ] 키보드로 모든 기능 접근 가능
- [ ] 스크린리더 호환 (aria-label, role)
- [ ] 충분한 색상 대비 (4.5:1 이상)
- [ ] 폼 필드에 명시적 label 연결

## Changelog
- 2026-03-20: Initial UX standards document created
