# Test Coverage Status & Targets

## Current Status (2026-03-27)

### Frontend Unit Tests
- **Result**: 15 PASS / 0 FAIL
- **Verification**: `cd frontend && npm test -- --run`
- **Files**: `frontend/src/shared/components/__tests__/*.test.tsx`
- **Framework**: Vitest + @testing-library/react

### Frontend E2E Tests
- **Files**: 12 spec files in `frontend/src/test/e2e/`
- **Framework**: Playwright
- **Status**: 미검증 (백엔드 구동 필요)

### Backend Unit/Integration Tests
- **Files**: 29 test files
- **Framework**: JUnit5 + Spring Boot Test + TestContainers
- **Status**: VERIFIED (`./gradlew :app:test` => 225 PASS / 0 FAIL)
- **Location**: `app/src/test/kotlin/`, `modules/approval/src/test/`, `platform/*/src/test/`

### Backend E2E Test Scenarios (existing)
- P2P flow (PR → PO → GR → JE)
- O2C flow (SO → GI → JE)
- Production flow (WO → material issue → completion)
- Budget management
- CRM pipeline
- Costing allocation
- Currency revaluation
- Financial close
- Multi-currency transactions

---

## Coverage Targets

### 100% Coverage (금액/로직 핵심)
> 한 줄이라도 놓치면 재무 오류로 이어지는 영역

- [ ] 금액 계산 (단가 × 수량, 할인, 세금)
- [ ] 재고 수불 (입고/출고/이동/조정)
- [ ] 전기/역분개 (차변=대변 검증)
- [ ] 문서 채번 (DocumentNumberGenerator)
- [ ] 권한 판단 (FieldPermission, DataScope, Role)
- [ ] 기간 마감 (FiscalPeriod open/close/reopen)
- [ ] 환산 (ExchangeRate 적용, 재평가)
- [ ] 워크플로우 엔진 (ApprovalService 상태 전이)
- [ ] 감가상각 계산 (DepreciationSchedule)

### 90%+ Coverage (서비스/애플리케이션 계층)
- [ ] 모든 Service 클래스
- [ ] 모든 Controller 클래스 (happy path + error path)

### 80%+ Coverage (UI 컴포넌트)
- [ ] 공통 컴포넌트 (DataGrid, ConfirmDialog, StatusBadge, etc.)
- [ ] 각 모듈 페이지 (리스트/상세/생성)

### 100% Contract Coverage
- [ ] OpenAPI spec과 실제 API 일치 검증 (자동화)
- [ ] 프런트 API 클라이언트와 백엔드 경로/타입 일치 검증
- [x] Flyway 마이그레이션 순서/충돌 검증

### E2E Scenarios
- [ ] P2P 전 구간 (PR → 결재 → RFQ → PO → GR → 검수 → Invoice → JE)
- [ ] O2C 전 구간 (Quote → SO → GI → Invoice → 수금)
- [ ] 생산 전 구간 (BOM → WO → 자재불출 → 실적 → 원가)
- [ ] 결산 전 구간 (기간마감 → 자동분개 → 재평가 → 마감확정)
- [ ] 알림 플로우 (이벤트 → 알림 생성 → 사용자 확인 → 읽음)
- [ ] 권한 플로우 (역할 → 메뉴 → 필드 → 데이터 범위)
- [ ] 멀티테넌시 (테넌트 격리 검증)

### Mutation Testing (핵심 도메인)
- [ ] 금액 계산 로직
- [ ] 상태 전이 로직 (문서 상태, 결재 상태)
- [ ] 재고 수불 로직

---

## Test Infrastructure TODO

- [x] BUG-002 수정 (vitest JSX transform)
- [ ] CI에서 백엔드 테스트 실행 확인
- [ ] 프런트 테스트 CI 게이트 활성화
- [ ] 커버리지 리포트 자동 생성 (vitest coverage-v8, JaCoCo)
- [ ] PR 단위 커버리지 diff 리포팅

## Changelog
- 2026-03-20: Initial test coverage document created
- 2026-03-23: Updated frontend unit tests and backend app test status with local verification results
- 2026-03-23: Expanded backend coverage to 195 passing tests after adding DataScope regression cases and rerunning `./gradlew :app:test`
- 2026-03-23: Added `FlywayPostgresMigrationTest`, validated PostgreSQL migrations through `V8`, and raised backend verification to 196 passing tests
- 2026-03-24: Added plant-only production DataScope regressions and raised backend verification to 198 passing tests
- 2026-03-25: Added budget-item update/transfer DataScope regressions and raised backend verification to 200 passing tests
- 2026-03-25: Added HR employee and cost-center DataScope regressions and raised backend verification to 202 passing tests
- 2026-03-25: Added asset DataScope regressions and raised backend verification to 204 passing tests
- 2026-03-25: Added quality/planning DataScope regressions and raised backend verification to 206 passing tests
- 2026-03-25: Added CRM DataScope regressions and raised backend verification to 208 passing tests
- 2026-03-25: Added cost-allocation DataScope regressions and raised backend verification to 210 passing tests
- 2026-03-25: Added asset schedule/summary/depreciation DataScope regressions and raised backend verification to 212 passing tests
- 2026-03-25: Added journal-entry company-scope regressions and raised backend verification to 214 passing tests
- 2026-03-25: Added BOM plant-scope regressions and raised backend verification to 216 passing tests
- 2026-03-26: Added standard/product costing DataScope regressions and raised backend verification to 218 passing tests
- 2026-03-26: Reran frontend `npm run lint`, `npm run build`, and `npm test -- --run` after aligning `CostingPage` to the updated costing contract; frontend remains 15 passing tests
- 2026-03-26: Added period-close company-scope regressions, updated period-close E2E/unit tests for the new `companyCode` contract, and raised backend verification to 220 passing tests
- 2026-03-26: Added currency revaluation company-scope regressions, updated currency flow tests for the new `companyCode` contract, and raised backend verification to 222 passing tests
- 2026-03-26: Reran frontend `npm run lint`, `npm run build`, and `npm test -- --run` after aligning `currencyApi` and `CurrencyPage` to the current currency/revaluation contract; frontend remains 15 passing tests
- 2026-03-27: Added batch-job company-scope regressions, updated batch flow tests for the new scope axes contract, and raised backend verification to 224 passing tests
- 2026-03-27: Reran frontend `npm run lint`, `npm run build`, and `npm test -- --run` after aligning `batchApi` and `BatchPage` to the current batch job / execution contract; frontend remains 15 passing tests
- 2026-03-27: Refined batch scope matching so organization scopes can still see company-tagged batch jobs, added a regression for that fallback path, and raised backend verification to 225 passing tests
