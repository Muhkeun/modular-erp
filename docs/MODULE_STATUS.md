# Module Status Matrix

> 각 모듈의 구현 상태를 Backend(BE) / Frontend(FE) / Test / Integration 축으로 추적합니다.

## Status Values
- `0` = 없음
- `1` = Skeleton (구조만)
- `2` = Basic CRUD
- `3` = Business Logic 포함
- `4` = Production Ready (테스트/에러처리/i18n 완비)

## Platform Modules

| Module | BE | FE | Test | Integration | Notes |
|--------|----|----|------|-------------|-------|
| core | 3 | - | 0 | - | BaseEntity, DomainEvent OK |
| security | 3 | 2 | 2 | 2 | JWT OK, SSO 미완, RateLimit OK |
| i18n | 1 | 3 | 0 | 0 | BE: TranslationId만. FE: 667키 |
| messaging | 2 | - | 0 | 1 | InProcess만, 외부 채널 없음 |
| web | 3 | - | 0 | 3 | ApiResponse, ExceptionHandler OK |
| preference | 3 | 2 | 0 | 0 | BE OK, App init load + grid preference wiring 완료 |
| admin | 3 | 3 | 1 | 2 | MenuProfile/DataScope/FieldPermission/GridPreference FE 연결 확대, DataScope 회귀 테스트 추가 |
| audit | 3 | 2 | 0 | 1 | 기본 동작 가능 |
| report | 3 | 2 | 2 | 2 | CSV/Excel/PDF OK |
| ai | 3 | 2 | 2 | 1 | Chat/Query/Embedding |

## Business Modules

| Module | BE | FE | Test | Integration | E2E Flow | Notes |
|--------|----|----|------|-------------|----------|-------|
| master-data | 3 | 3 | 1 | 2 | - | Item/BOM OK, Vendor/Customer 없음 |
| purchase | 3 | 3 | 1 | 3 | PARTIAL | PR/PO + DataScope(search/detail/action/create) 적용, org-scope create 회귀 테스트 추가 |
| sales | 3 | 2 | 1 | 2 | PARTIAL | SO 기본 + DataScope(search/detail/action/create) 적용 |
| logistics | 3 | 3 | 1 | 3 | PARTIAL | GR/GI/Stock 기본 + DataScope 적용. 로트/시리얼 없음 |
| production | 2 | 2 | 1 | 2 | - | WO 기본 + DataScope(search/detail/action/create) 적용, detail/action 403 회귀 테스트 추가. WorkCenter/Routing API만 |
| planning | 2 | 2 | 0 | 1 | - | MRP 기본. Schedule/Capacity API만, FE 없음 |
| account | 3 | 2 | 1 | 1 | PARTIAL | JournalEntry OK. 자동분개 미완 |
| hr | 2 | 2 | 0 | 0 | - | Employee 기본만 |
| approval | 3 | 2 | 2 | 2 | PARTIAL | 결재/위임 구현. 다단계 검증 필요 |
| notification | 3 | 2 | 1 | 0 | - | Notification API contract aligned |
| document | 2 | 0 | 0 | 1 | - | 번호채번 서비스만, UI 없음 |
| budget | 3 | 2 | 1 | 1 | - | 기본 예산 관리 |
| asset | 3 | 2 | 1 | 1 | - | 감가상각/처분 기본 |
| period-close | 3 | 2 | 1 | 1 | - | 마감 체크리스트 기본 |
| batch | 2 | 2 | 0 | 0 | - | 배치 실행 기본 |
| costing | 3 | 1 | 1 | 1 | - | FE 라우트 복구 완료 |
| crm | 3 | 2 | 1 | 1 | - | Lead/Opportunity 기본 |
| currency | 3 | 2 | 1 | 1 | - | 환율/재평가 기본 |
| contract | 1 | 0 | 0 | 0 | - | Skeleton |
| quality | 1 | 0 | 0 | 0 | - | Skeleton |
| supply-chain | 1 | 0 | 0 | 0 | - | Skeleton |

## Overall Score
- **Platform**: 평균 2.4/4 - 구조는 있으나 FE 연결/테스트 부족
- **Business**: 평균 1.8/4 - 기본 CRUD 수준, E2E 플로우 미완

## Changelog
- 2026-03-20: Initial status matrix
- 2026-03-20: Updated admin/purchase/sales/logistics/production integration status after DataScope rollout
- 2026-03-20: Recorded targeted DataScope enforcement regression coverage
- 2026-03-23: Updated preference/admin/notification/costing notes after frontend integration and contract fixes
