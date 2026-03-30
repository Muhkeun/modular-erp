# Gap Analysis

> 확인된 버그(BUGS.md)가 아닌, 구조적 미흡/누락/설계 갭을 기록합니다.
> 상용 SaaS ERP 수준 도달을 기준으로 평가합니다.

## 1. Platform Feature Gaps (플랫폼 연결 누락)

### GAP-P01: MenuProfile API가 UI 네비게이션에 미연결
- **Severity**: P1
- **Backend**: `MenuProfileController.kt` (`/api/v1/admin/menu-profiles`) + `adminApi.ts` 존재
- **Frontend**: `MainLayout.tsx` 사이드바 메뉴가 전부 하드코딩
- **Expected**: 로그인 시 사용자 역할의 MenuProfile을 로드해서 동적 네비게이션 구성
- **Impact**: 역할별 메뉴 제어 불가, SaaS 다중 테넌트 메뉴 커스터마이징 불가

### GAP-P02: FieldPermission 훅 미사용
- **Severity**: P1
- **Detail**: `useFieldPermission.ts` 구현됨 (FULL/READONLY/MASKED/HIDDEN 지원) 하지만 어떤 페이지에서도 호출되지 않음
- **Impact**: 필드 레벨 권한 제어 무의미

### GAP-P03: GridPreference 훅 미사용
- **Severity**: P2
- **Detail**: `useGridPreference.ts` 구현됨 (AG Grid 상태 자동 저장) 하지만 어떤 DataGrid에서도 연결되지 않음
- **Impact**: 사용자 그리드 설정 유실

### GAP-P04: DataScope 적용 범위가 아직 부분적임
- **Severity**: P2
- **Detail**:
  - UI는 주요 리스트와 일부 상세 접근에 연결됨
  - 서버 검색 API는 `purchase-requests`, `purchase-orders`, `sales-orders`, `goods-receipts`, `goods-issues`, `work-orders`, `stock`, `work-centers`, `routings`, `boms`, `quality-inspections`, `mrp-runs`, `employees`, `cost-centers`, `standard-costs`, `product-costs`, `assets`, `journal-entries`, `period-close`, `currency-revaluations`, `batch-jobs`, `crm-customers`, `crm-leads`, `crm-opportunities`, `crm-activities`에 적용되며 `budget-items`는 period별 조회에서 `department/plant` 스코프로 필터링됨
  - 주요 문서형 리소스와 일부 비핵심 마스터의 `getById`, 집계, ID 기반 상태변경 API도 동일한 DataScope로 사전 차단되며 `period-close`는 checklist/close/closing-entry까지, `currency revaluations`는 post/reverse까지, `batch jobs`는 execute/history/status/cancel/retry까지 가드되고 저장된 `department/plant/company` 축 중 가장 구체적인 값으로 매칭됨
  - 주요 생성 API도 `company/plant/department` payload를 같은 DataScope로 사전 검증하며 `budget transfer`, `cost allocation`, `standard-cost`, `product-cost calculate`, `period-close generate/closing-entry`, `currency revaluation create`, `batch job create`는 연결된 대상 리소스까지 스코프 검증함
  - `ORGANIZATION`은 조직 트리 하위의 `company/plant/department` 코드 집합으로 확장됨
  - `app:test`의 `DataScopeEnforcementTest`로 조직 스코프 기반 PR 생성 허용/차단, PLANT 스코프 기반 WO 상세/액션 403, DEPARTMENT 스코프 기반 PR 삭제 차단, PR->PO 전환의 교차 리소스 차단, work-center 조직 스코프 검색/생성, BOM 조직 스코프 검색/생성, routing release 차단, budget item 수정 차단, budget transfer 교차 스코프 차단, employee 생성 차단, cost-center 삭제 차단, standard/product cost 검색 필터링, standard cost 수정 차단, product cost 계산 차단, asset 생성/폐기 차단, asset schedule 차단, asset depreciation 대상 축소, quality inspection 완료 차단, MRP 실행 차단, journal entry company 검색 필터링, journal entry 상세/전기 차단, BOM 상세/explode 차단, currency revaluation 검색 필터링, currency revaluation 전기 차단, batch job 검색 필터링, batch job 실행 차단, CRM customer 생성 차단, CRM opportunity 단계변경 차단, cost allocation 생성/전기 차단을 회귀 고정함
  - `DEPARTMENT` 서버 필터는 `purchase-requests`처럼 명시적 `departmentCode`가 있는 리소스만 지원하고, `OWN` 서버 필터는 CRM 리소스의 `assignedTo` 축에 적용됨
  - `stock`, `work-centers`, `routings`, `quality-inspections`, `mrp-runs`, `capacity-plans`, `production-schedule`는 도메인 축 제약으로 `PLANT`만 서버 지원하지만 organization scope는 하위 plant 코드로 좁혀서 적용함
- **Impact**: 핵심 문서 리소스는 많이 닫혔지만, 모든 모듈/모든 쓰기 연산으로 확장되진 않음

### GAP-P05: i18n 백엔드 미완
- **Severity**: P1
- **Detail**: `platform/i18n`에 `TranslationId.kt` 하나만 존재. 서비스/API/레포지토리 없음
- **Frontend**: i18next로 ko.json/en.json 667키 관리 중 (프런트만 동작)
- **Target**: 4개국어 이상 + 서버사이드 메시지/이메일 템플릿 i18n
- **Impact**: 백엔드 에러메시지/이메일 템플릿 등은 i18n 불가

### GAP-P06: SSO 콜백 미구현
- **Severity**: P2
- **Detail**: `SsoController.kt`에 callback 엔드포인트 구조만 있고 실제 토큰 교환 로직 미완
- **Impact**: 외부 IdP 연동 불가

### GAP-P07: 파일 관리 모듈 부재
- **Severity**: P1
- **Detail**: 파일 업로드/다운로드/미리보기/대량 다운로드 기능 없음
- **Expected**: 공통 파일 서비스 (첨부파일, 이미지, 문서) + 클라우드 스토리지 연동
- **Impact**: 모든 비즈니스 모듈에서 첨부파일 처리 불가

### GAP-P08: 계층형 마스터 조회 부재
- **Severity**: P1
- **Detail**: 회사→공장→저장소→빈, 국가→도시, 품목분류 계층 등 연쇄 조회 API 없음
- **Impact**: 마스터 데이터 입력 시 상위→하위 필터링 불가

### GAP-P09: 주소/우편번호 검색 부재
- **Severity**: P2
- **Detail**: 주소 검색 기능 없음. 구현 시 juso.go.kr (정부 공공 API) 우선 + AddressSearchPort 추상화
- **참고**: TECH_DECISIONS.md "주소검색" 섹션에 프로바이더 평가 완료

## 2. Frontend-Backend Contract Gaps

### GAP-C01: 라우트 없는 메뉴 항목들
- **Pages without routes**: `/costing` (MainLayout에 있으나 App.tsx에 라우트 없음)
- **API without UI**: quality inspections, supply-chain evaluations, contracts, planning/schedule, planning/capacity, production/work-centers, production/routings
- **UI without API**: 일부 admin 기능의 세부 CRUD

### GAP-C02: 프런트 API 클라이언트 누락 모듈
- **Status**: 사실상 해소 (2026-03-23)
- **완료**:
  - quality, supply-chain, contract
  - production (`work-centers`, `routings`, `work-orders`)
  - planning (`mrp`, `schedule`, `capacity`)
  - report, export
  - sales, logistics, account, hr, purchase
  - budget, asset, batch, period-close, crm, currency
- **남음**:
  - `document`는 번호 채번 서비스라 별도 FE 클라이언트 필요성만 재판단하면 됨

## 3. Business Logic Gaps

### GAP-B01: P2P 플로우 불완전
- PR → 결재 → RFQ → 입찰비교 → PO → 입고 → 검수 → 송장 → 분개
- **Missing**: RFQ 발송/수신, 입찰비교, 3-way match (PO-GR-Invoice), 자동 분개

### GAP-B02: O2C 플로우 불완전
- 견적 → 수주 → 출고 → 송장 → 수금
- **Missing**: 견적(Quote), 출고 시 재고 차감 연동, 매출 송장, 수금/외화

### GAP-B03: 재고 관리 기본 기능 부족
- **Missing**: 로트/시리얼 추적, 재고 이동, 재고 실사, 바코드, 예약/가용재고 계산

### GAP-B04: 회계 자동화 미완
- **Missing**: 자동 분개 규칙, 기간 마감 자동 체크, 원가 계산 → GL 반영

### GAP-B05: Vendor/Customer 독립 마스터 부재
- Vendor는 supply-chain에 VendorEvaluation만 있고 기본 CRUD 없음
- Customer는 CRM 모듈에 포함되어 있으나 마스터 데이터로서의 독립성 부족

### GAP-B06: 멀티컴퍼니 인증 플로우 부재
- **Detail**: 로그인 후 회사 선택/전환 기능 없음
- **Expected**: 사용자가 접근 가능한 회사 목록 → 선택 → 해당 회사 컨텍스트 적용

### GAP-B07: HR 모듈 미완
- **Detail**: Employee 기본 CRUD만 존재. 인사발령, 휴가관리, 근태 없음

### GAP-B08: 결재 워크플로우 세부 흐름 미검증
- **Detail**: 위임, 합의, 통보, 반려 후 재상신 등 복합 시나리오 테스트 부재

## 4. UX/Quality Gaps (상세는 UX_STANDARDS.md 참조)

### GAP-U01: 역할 기반 홈/대시보드 부재
### GAP-U02: 리스트 화면에 Saved View / Filter Preset 없음
### GAP-U03: Bulk Action / Inline Edit 부재
### GAP-U04: 모바일/태블릿 대응 미완
### GAP-U05: 알림이 업무 액션과 미연결 (알림 클릭 → 해당 문서 이동 등)

## Changelog
- 2026-03-20: Initial gap analysis
- 2026-03-20: Updated GAP-P04 after UI hookup and server-side scope-aware search rollout
- 2026-03-20: Added DataScope regression coverage note for organization expansion and detail/action denial
- 2026-03-23: Updated GAP-C02 after adding missing frontend API clients for contract/quality/supply-chain/production/planning endpoints
- 2026-03-23: Narrowed GAP-C02 to the document-numbering client decision after switching remaining module pages to their API wrappers
- 2026-03-23: Expanded GAP-P04 regression notes with department delete denial and cross-resource PR->PO scope enforcement
- 2026-03-24: Extended GAP-P04 notes for plant-only stock/production resources and added new production regression scenarios
- 2026-03-25: Extended GAP-P04 notes for budget-item update/transfer enforcement and added new budget regression scenarios
- 2026-03-25: Extended GAP-P04 notes for HR employees and costing cost-centers and added new department-scope regression scenarios
- 2026-03-25: Extended GAP-P04 notes for assets and added new department-scope asset regression scenarios
- 2026-03-25: Extended GAP-P04 notes for quality inspections and planning plant resources and added new quality/MRP regression scenarios
- 2026-03-25: Extended GAP-P04 notes for CRM own-scope resources and added new CRM assigned-user regression scenarios
- 2026-03-25: Extended GAP-P04 notes for cost allocations and added new cost-center bridge regression scenarios
- 2026-03-25: Extended GAP-P04 notes for asset schedule/summary/depreciation endpoints and added new asset scope regression scenarios
- 2026-03-25: Extended GAP-P04 notes for journal entries and added new company-scope journal entry regression scenarios
- 2026-03-25: Extended GAP-P04 notes for BOM APIs and added new plant-scope BOM regression scenarios
- 2026-03-26: Extended GAP-P04 notes for standard/product costing resources and added new department-scope costing regression scenarios
- 2026-03-26: Extended GAP-P04 notes for period-close resources and added new company-scope period search/detail/write regression scenarios
- 2026-03-26: Extended GAP-P04 notes for currency revaluations and added new company-scope currency search/post regression scenarios
- 2026-03-27: Extended GAP-P04 notes for batch jobs and added new company-scope batch search/execute regression scenarios
- 2026-03-27: Refined batch job scope matching for company-tagged jobs under organization scope and added a fallback visibility regression scenario
