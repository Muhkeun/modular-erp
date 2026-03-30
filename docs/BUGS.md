# Known Bugs

> 재현 가능한 버그만 기록합니다. 추측은 GAP_ANALYSIS.md에 기록합니다.

## Status Legend
- `OPEN` - 미수정
- `IN_PROGRESS` - 수정 중
- `FIXED` - 수정 완료
- `VERIFIED` - 수정 후 테스트 통과 확인

---

### BUG-001: Notification API 경로 불일치 (P0)
- **Status**: FIXED
- **Frontend**: `notificationApi.ts` → `/api/v1/notifications` (plural), GET/PUT/DELETE + recipientId/userId 파라미터 정합성 반영
- **Backend**: `NotificationController.kt` → `/api/v1/notifications` (plural), GET/PUT/DELETE + recipientId/userId 파라미터
- **Impact**: 알림 기능 전체 동작 불가
- **Fix**: 프런트엔드 API 경로를 백엔드에 맞추고, 메서드/파라미터 정합성 확보

### BUG-002: 프런트엔드 테스트 전체 실패 (P0)
- **Status**: VERIFIED
- **Detail**: `npm test -- --run` 기준 15개 컴포넌트 테스트가 모두 통과함
- **Files**: `frontend/src/shared/components/__tests__/*.test.tsx`
- **Cause**: JSX transform 설정 정비 및 테스트 파일 정합성 보정
- **Impact**: CI/CD 파이프라인에서 테스트 게이트 무의미

### BUG-003: 죽은 네비게이션 링크 (P0)
- **Status**: FIXED
- **Detail**: `App.tsx`에 `/costing` 라우트가 추가되고 공통 navigation 정의와 연결됨
- **Impact**: 사용자가 Costing 메뉴 클릭 시 404 또는 빈 화면
- **Related**: `CommandPalette.tsx`도 공통 navigation 정의를 사용하도록 정리됨

### BUG-004: ESLint 53 errors (P1)
- **Status**: VERIFIED
- **Detail**: `npm run lint` 통과
- **Impact**: 코드 품질 게이트 실패, 잠재 런타임 버그

### BUG-005: User Preference 서버 값 미로딩 (P1)
- **Status**: FIXED
- **Detail**: 인증 후 `App.tsx` 초기화 시점에 `usePreferenceStore.load()`가 호출되도록 연결됨
- **Files**: `frontend/src/shared/hooks/usePreference.ts`
- **Impact**: 새로고침 시 사용자 설정이 기본값으로 초기화

### BUG-006: README 테스트 통과 수 과장 (P2)
- **Status**: FIXED
- **Detail**: README/README.ko 테스트 배지와 테스트 섹션을 현재 검증 결과에 맞게 수정
- **Impact**: 프로젝트 신뢰도 훼손

### BUG-007: 문서번호 유니크 키가 멀티테넌트 범위를 무시함 (P1)
- **Status**: VERIFIED
- **Detail**: `PurchaseRequest`, `PurchaseOrder`, `SalesOrder`, `WorkOrder`, `GoodsReceipt`, `GoodsIssue`, `JournalEntry`, `QualityInspection`, `Contract`, `RFQ`의 문서번호 유니크 키를 `tenant_id + document_no`로 변경하고 PostgreSQL용 Flyway 마이그레이션 `V8__tenant_scoped_document_numbers.sql` 추가
- **Impact**: 다른 테넌트가 같은 월 시퀀스 문서번호를 생성할 때 500 오류 발생
- **Fix**: 전표 엔티티의 유니크 제약을 멀티테넌트 기준으로 수정하고 `DataScopeEnforcementTest` 회귀 시나리오와 `FlywayPostgresMigrationTest`로 PostgreSQL 마이그레이션까지 검증

---

## Changelog
- 2026-03-20: Initial bug list created from codebase analysis
- 2026-03-23: Marked BUG-002 and BUG-004 as VERIFIED after `npm test -- --run` and `npm run lint`
- 2026-03-23: Marked BUG-001, BUG-003, BUG-005, BUG-006 as FIXED after API/navigation/preference/docs alignment
- 2026-03-23: Added BUG-007 and marked it VERIFIED after fixing tenant-scoped document number uniqueness and rerunning `./gradlew :app:test`
- 2026-03-23: Verified BUG-007 against PostgreSQL Flyway migrations and corrected a PostgreSQL-only syntax issue in `V6__approval_enhanced.sql`
