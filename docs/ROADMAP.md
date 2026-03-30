# Execution Roadmap

> 상용 SaaS ERP 프레임워크 완성을 목표로 한 우선순위별 로드맵입니다.

## Phase 0: 신뢰성 확보 (P0)
> 기존 코드의 장애급 이슈 제거. 이 단계 없이 기능 추가는 부채만 쌓임.

- [x] **BUG-001** Notification API 경로/메서드 불일치 수정
- [x] **BUG-002** 프런트엔드 테스트 환경 수정 (React is not defined)
- [x] **BUG-003** 죽은 네비게이션 링크 수정 (costing 라우트 추가)
- [x] **BUG-004** ESLint 53 errors 해소
- [x] **BUG-005** usePreferenceStore.load() 앱 초기화 시 호출
- [x] **BUG-006** README 테스트 수치 실제 반영
- [x] **GAP-P02** useFieldPermission 최소 1개 페이지에 적용
- [x] **GAP-P03** useGridPreference DataGrid에 연결
- [x] **GAP-C01** 라우트-메뉴 정합성 100% 달성
- [x] **GAP-C02** 백엔드 API 있는 모듈의 프런트 API 클라이언트 생성

## Phase 1: 핵심 플랫폼 기능 완성 (P1)
> SaaS 멀티테넌트 ERP의 기반이 되는 플랫폼 기능

- [x] **GAP-P01** MenuProfile → 동적 네비게이션 연결
- [ ] **GAP-P05** i18n 백엔드 서비스/API 구현 (최소 ko/en)
- [ ] **GAP-P07** 공통 파일 관리 모듈 (업로드/다운로드/미리보기)
- [ ] **GAP-P08** 계층형 마스터 조회 API (회사→공장→저장소, 품목분류 등)
- [ ] **GAP-P09** 주소검색 모듈 (juso.go.kr 어댑터 + AddressSearchPort 추상화)
- [ ] **GAP-B06** 멀티컴퍼니 인증 플로우 (로그인 후 회사 선택/전환)
- [~] **GAP-P04** DataScope UI 연결 + 주요 검색 API 반영
  - 완료: 주요 리스트/상세 fail-closed, server-side scope-aware search, getById/ID action server enforcement, create payload 검증
  - 완료: organization hierarchy -> company/plant/department expansion
  - 완료: `app:test` 회귀 테스트로 organization create guard + work-order detail/action denial + department delete denial + PR->PO bridge denial + plant-only production resource guard + budget item update/transfer guard + employee/cost-center/asset guard 고정
  - 완료: `stock`, `work-centers`, `routings`는 organization scope를 하위 plant 코드로 좁혀서 적용
  - 완료: `budget-items`는 period별 조회, 상세, 수정, 이체에 `department/plant` 스코프 적용
  - 완료: `employees`는 company/department 스코프, `cost-centers`는 department 스코프로 조회/상세/쓰기 가드 적용
  - 완료: `assets`는 department 스코프로 검색/상세/생성/수정/활성화/폐기 가드 적용
  - 완료: `asset schedule`, `asset summary`, `depreciation run`도 department 스코프로 조회/집계/실행 대상을 축소
  - 완료: `journal-entries`는 company 스코프로 검색/상세/생성/전기/역분개 가드 적용
  - 완료: `boms`는 plant 스코프로 검색/상세/생성/확정/explode 가드 적용
  - 완료: `standard-costs`, `product-costs`는 `costCenterCode -> department` 축으로 검색/상세/생성/수정/계산 가드 적용
  - 완료: `currency revaluations`는 company 스코프로 검색/생성/전기/역분개 가드 적용
  - 완료: `batch jobs`는 `company/plant/department` 축으로 검색/상세/생성/실행/히스토리/상태 액션 가드 적용
  - 완료: `quality-inspections`, `mrp-runs`, `capacity-plans`, `production-schedule`는 plant 스코프로 검색/상세/생성/액션 접근 가드 적용
  - 완료: `crm-customers`, `crm-leads`, `crm-opportunities`, `crm-activities`는 `assignedTo` 기준 OWN 스코프로 검색/상세/쓰기/액션 접근 가드 적용
  - 완료: `cost allocations`는 연결된 `from/to cost-center` 둘 다 department 스코프를 통과해야 검색/생성/전기 가능
  - 남음: 일부 tenant-global 운영 리소스의 정책 명시, update/delete payload 검증 확대
- [ ] **GAP-P06** SSO 콜백 구현 (최소 Google OIDC)
- [ ] 공통 페이지 패턴 확립 (리스트/상세/생성 표준 레이아웃)

## Phase 2: P2P 플로우 완성 (핵심 비즈니스 #1)
> 구매요청 → 결재 → RFQ → 발주 → 입고 → 검수 → 송장 → 분개

- [ ] **GAP-B01** RFQ 발송/수신 플로우
- [ ] **GAP-B01** 입찰비교 기능
- [ ] **GAP-B01** 3-way match (PO-GR-Invoice)
- [ ] **GAP-B01** 자동 분개 규칙 엔진
- [ ] **GAP-B05** Vendor 독립 마스터 CRUD
- [ ] Quality Inspection 모듈 구현 (입고 검수)
- [ ] P2P E2E 테스트 (전 구간)

## Phase 3: Inventory + WMS 강화
> 재고 관리는 P2P/O2C 모두의 기반

- [ ] **GAP-B03** 로트/시리얼 추적
- [ ] **GAP-B03** 재고 이동 (창고 간, 저장소 간)
- [ ] **GAP-B03** 재고 실사 (Physical Inventory)
- [ ] **GAP-B03** 예약/가용재고 계산
- [ ] 바코드/QR 스캔 지원 (모바일 UX)
- [ ] Inventory E2E 테스트

## Phase 4: O2C 플로우 완성 (핵심 비즈니스 #2)
> 견적 → 수주 → 출고 → 매출 송장 → 수금

- [ ] **GAP-B02** 견적(Quote) 모듈
- [ ] **GAP-B02** 출고 시 재고 차감 연동
- [ ] **GAP-B02** 매출 송장
- [ ] **GAP-B02** 수금/외화 처리
- [ ] **GAP-B05** Customer 독립 마스터 정비
- [ ] O2C E2E 테스트

## Phase 5: Finance 고도화
> 자동 분개, 기간 마감, 원가 반영, 다중 원장

- [ ] **GAP-B04** 자동 분개 규칙 관리 UI
- [ ] **GAP-B04** 기간 마감 자동 체크리스트
- [ ] **GAP-B04** 원가 계산 → GL 반영
- [ ] 다중 원장 (관리회계/재무회계)
- [ ] 고정자산 감가상각 자동 실행
- [ ] Financial Close E2E 테스트

## Phase 6: Production + MRP
> BOM → 작업지시 → 자재불출 → 실적 → 원가반영

- [ ] WorkCenter/Routing 프런트엔드 구현
- [ ] 작업지시 → 자재 자동 불출
- [ ] 생산 실적 보고
- [ ] MRP → 자동 PR/WO 생성
- [ ] Production E2E 테스트

## Phase 7: 운영 UX 고도화
> 상용 SaaS 수준의 UX

- [ ] **GAP-U01** 역할 기반 대시보드 (구매담당자, 창고관리자, 경영진 등)
- [ ] **GAP-U02** Saved View / Filter Preset
- [ ] **GAP-U03** Bulk Action / Inline Edit
- [ ] **GAP-U05** 알림 → 문서 직접 이동
- [ ] Global Search (문서번호, 품목, 거래처 통합검색)
- [ ] 키보드 중심 입력 최적화
- [ ] **GAP-U04** 모바일/태블릿 반응형

## Phase 8: SaaS 프레임워크화
> 설정형 플랫폼으로 전환

- [ ] 워크플로우 디자이너 실제 편집 기능
- [ ] 문서 양식 커스터마이징
- [ ] 알림 템플릿 관리 UI
- [ ] 테넌트별 Feature Flag
- [ ] 통합 허브 (웹훅, API 키, 이벤트 아웃박스)
- [ ] 산업별 패키지 (제조, 유통, 서비스)

---

## Test Coverage Target by Phase

| Phase | 백엔드 커버리지 | 프런트 커버리지 | E2E |
|-------|----------------|----------------|-----|
| 0 | 기존 테스트 통과 | 기존 테스트 통과 | - |
| 1 | 플랫폼 모듈 80%+ | 공통 컴포넌트 80%+ | 로그인/네비게이션 |
| 2 | purchase/logistics/account 90%+ | 해당 페이지 80%+ | P2P 전 구간 |
| 3 | logistics 90%+ | 재고 페이지 80%+ | 재고 이동/실사 |
| 4 | sales/account 90%+ | 해당 페이지 80%+ | O2C 전 구간 |
| 5 | 금액계산/분개/마감 **100%** | - | 결산 전 구간 |
| 6 | production/planning 90%+ | 해당 페이지 80%+ | 생산 전 구간 |
| 7-8 | 유지 | 유지 | 전체 시나리오 |

## Changelog
- 2026-03-20: Initial roadmap created
- 2026-03-20: Marked GAP-P04 as partially completed with server-side search filtering
- 2026-03-20: Added targeted DataScope regression test milestone
- 2026-03-23: Checked off Phase 0 fixes for notification API, testing, routing, lint, preferences, README, field permissions, grid preferences, and menu-route parity
- 2026-03-23: Marked GAP-P01 done after resolved menu profile navigation and route guards were wired into the frontend
- 2026-03-23: Marked GAP-C02 done after adding API clients for contract/quality/supply-chain/production/planning backend endpoints
- 2026-03-23: Finished the remaining GAP-C02 page migrations by switching budget/asset/batch/period-close/crm/currency screens to module API clients
- 2026-03-23: Expanded GAP-P04 regression coverage and fixed tenant-scoped document number uniqueness for generated business documents
- 2026-03-23: Added Flyway `V8` migration so the tenant-scoped document number uniqueness fix is applied to PostgreSQL environments too
- 2026-03-23: Added PostgreSQL Flyway smoke coverage and fixed a `V6__approval_enhanced.sql` syntax error discovered while validating migrations end-to-end
- 2026-03-24: Extended GAP-P04 to plant-only stock/production resources and reran `./gradlew :app:test` with 198 backend tests passing
- 2026-03-25: Extended GAP-P04 to budget-item update/transfer enforcement and reran `./gradlew :app:test` with 200 backend tests passing
- 2026-03-25: Extended GAP-P04 to HR employees and costing cost-centers and reran `./gradlew :app:test` with 202 backend tests passing
- 2026-03-25: Extended GAP-P04 to assets and reran `./gradlew :app:test` with 204 backend tests passing
- 2026-03-25: Extended GAP-P04 to quality inspections and planning plant resources, then reran `./gradlew :app:test` with 206 backend tests passing
- 2026-03-25: Extended GAP-P04 to CRM own-scope resources and reran `./gradlew :app:test` with 208 backend tests passing
- 2026-03-25: Extended GAP-P04 to department-scoped cost allocations and reran `./gradlew :app:test` with 210 backend tests passing
- 2026-03-25: Extended GAP-P04 to asset schedule/summary/depreciation endpoints and reran `./gradlew :app:test` with 212 backend tests passing
- 2026-03-25: Extended GAP-P04 to company-scoped journal entries and reran `./gradlew :app:test` with 214 backend tests passing
- 2026-03-25: Extended GAP-P04 to plant-scoped BOM APIs and reran `./gradlew :app:test` with 216 backend tests passing
- 2026-03-26: Extended GAP-P04 to department-scoped standard/product costing resources, added `V9__costing_scope_axes.sql`, and reran `./gradlew :app:test` with 218 backend tests passing
- 2026-03-26: Aligned `CostingPage` with the current costing DTOs and endpoints, exposed the new `costCenterCode` axis in the UI, and reran frontend `lint/build/test`
- 2026-03-26: Extended GAP-P04 to company-scoped period close resources, added `V10__period_close_company_scope.sql`, and reran `./gradlew :app:test` with 220 backend tests passing
- 2026-03-26: Aligned `PeriodClosePage` and `periodCloseApi` with the current generate/checklist/closing-entry contract and reran frontend `lint/build/test`
- 2026-03-26: Extended GAP-P04 to company-scoped currency revaluations, added `V11__currency_revaluation_company_scope.sql`, and reran `./gradlew :app:test` with 222 backend tests passing
- 2026-03-26: Aligned `CurrencyPage` and `currencyApi` with the current currencies/exchange-rates/revaluations contract and reran frontend `lint/build/test`
- 2026-03-27: Extended GAP-P04 to scope-tagged batch jobs, added `V12__batch_scope_axes.sql`, and reran `./gradlew :app:test` with 224 backend tests passing
- 2026-03-27: Aligned `batchApi` and `BatchPage` with the current batch job / execution routes and DTO fields, then reran frontend `lint/build/test`
- 2026-03-27: Refined batch scope matching to accept the most specific populated axis (`department` -> `plant` -> `company`) and reran `./gradlew :app:test` with 225 backend tests passing
