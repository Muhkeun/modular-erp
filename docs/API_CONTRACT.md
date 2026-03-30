# API Contract Registry

> 프런트엔드와 백엔드 사이의 API 계약을 추적합니다.
> 불일치가 발견되면 즉시 기록하고, 수정 시 VERIFIED로 변경합니다.

## Mismatches (불일치)

### MISMATCH-001: Notification API (P0) → BUG-001
- **Status**: VERIFIED
- **Frontend** (`notificationApi.ts`):
  - `GET /api/v1/notifications`
  - `PUT /api/v1/notifications/{id}/read`
  - `PUT /api/v1/notifications/read-all`
  - `GET /api/v1/notifications/unread-count`
- **Backend** (`NotificationController.kt`):
  - `GET /api/v1/notifications` (params: recipientId, unreadOnly)
  - `PUT /api/v1/notifications/{id}/read`
  - `PUT /api/v1/notifications/read-all` (param: recipientId)
  - `DELETE /api/v1/notifications/{id}`
  - `GET /api/v1/notifications/unread-count` (param: recipientId)
- **Diff**: 경로/메서드/파라미터 정합성 확보 완료

---

## Endpoint Coverage Matrix

> `✅` = FE API 클라이언트 있음, `❌` = 없음, `⚠️` = 부분

| Backend Endpoint | FE Client | FE Page | Status |
|-----------------|-----------|---------|--------|
| `/api/v1/auth` | ✅ authApi | ✅ LoginPage | OK |
| `/api/v1/dashboard` | ✅ dashboardApi | ✅ DashboardPage | OK |
| `/api/v1/master-data/items` | ✅ adminApi | ✅ ItemListPage | OK |
| `/api/v1/master-data/boms` | ✅ adminApi | ✅ (within Item) | OK |
| `/api/v1/purchase/requests` | ✅ purchaseApi | ✅ PurchaseRequestPage | OK |
| `/api/v1/purchase/orders` | ✅ purchaseApi | ✅ PurchaseOrderPage | OK |
| `/api/v1/sales/orders` | ✅ salesApi | ✅ SalesOrderPage | OK |
| `/api/v1/logistics/goods-receipts` | ✅ logisticsApi | ✅ GoodsReceiptPage | OK |
| `/api/v1/logistics/goods-issues` | ✅ logisticsApi | ✅ GoodsIssuePage | OK |
| `/api/v1/logistics/stock` | ✅ logisticsApi | ✅ StockOverviewPage | OK |
| `/api/v1/production/work-orders` | ✅ productionApi | ✅ WorkOrderPage | OK |
| `/api/v1/production/work-centers` | ✅ productionApi | ❌ | API client only |
| `/api/v1/production/routings` | ✅ productionApi | ❌ | API client only |
| `/api/v1/planning/mrp` | ✅ planningApi | ✅ MrpPage | OK |
| `/api/v1/planning/schedule` | ✅ planningApi | ❌ | API client only |
| `/api/v1/planning/capacity` | ✅ planningApi | ❌ | API client only |
| `/api/v1/account/journal-entries` | ✅ accountApi | ✅ JournalEntryPage | OK |
| `/api/v1/hr/employees` | ✅ hrApi | ✅ HrPage | OK |
| `/api/v1/approvals` | ✅ approvalApi | ✅ ApprovalPage | OK |
| `/api/v1/admin/workflows` | ⚠️ | ✅ WorkflowDesignerPage | 부분 연결 |
| `/api/v1/notifications` | ✅ notificationApi | ✅ NotificationPage | OK |
| `/api/v1/budgets` | ✅ budgetApi | ✅ BudgetPage | OK |
| `/api/v1/assets` | ✅ assetApi | ✅ AssetPage | OK |
| `/api/v1/period-close` | ✅ periodCloseApi | ✅ PeriodClosePage | OK |
| `/api/v1/batch` | ✅ batchApi | ✅ BatchPage | OK |
| `/api/v1/costing` | ✅ costingApi | ✅ CostingPage | OK |
| `/api/v1/crm` | ✅ crmApi | ✅ CrmPage | OK |
| `/api/v1/currencies` | ✅ currencyApi | ✅ CurrencyPage | OK |
| `/api/v1/contracts` | ✅ contractApi | ❌ | API client only |
| `/api/v1/quality/inspections` | ✅ qualityApi | ❌ | API client only |
| `/api/v1/supply-chain/evaluations` | ✅ supplyChainApi | ❌ | API client only |
| `/api/v1/admin/roles` | ✅ adminApi | ✅ RoleManagementPage | OK |
| `/api/v1/admin/system-codes` | ✅ adminApi | ✅ SystemCodePage | OK |
| `/api/v1/admin/organizations` | ✅ adminApi | ✅ OrganizationPage | OK |
| `/api/v1/admin/audit-logs` | ✅ adminApi | ✅ AuditLogPage | OK |
| `/api/v1/admin/api-keys` | ✅ adminApi | ✅ ApiKeyPage | OK |
| `/api/v1/admin/tenants` | ✅ adminPhase4Api | ✅ TenantPage | OK |
| `/api/v1/admin/field-permissions` | ✅ adminApi | ❌ (관리 UI만) | UI 미연결 |
| `/api/v1/admin/menu-profiles` | ✅ adminApi | ✅ MainLayout / ProtectedRoute | OK |
| `/api/v1/admin/data-scopes` | ✅ adminPhase4Api | ⚠️ | 주요 리스트/상세 연결, 관리 UI 미구현 |
| `/api/v1/preferences` | ✅ preferenceApi | ✅ App init load | OK |
| `/api/v1/reports` | ✅ reportApi | ❌ | API client only |
| `/api/v1/export` | ✅ exportApi | ✅ ItemListPage | Partial |
| `/api/v1/ai` | ✅ aiApi | ✅ AiChatPage | OK |

---

## API Design Standards

신규 API 작성 시 준수 사항:

- 경로: `/api/v1/{module}/{resource}` (복수형)
- 응답: `ApiResponse<T>` wrapper 사용
- 페이징: `page`, `size`, `sort` 쿼리 파라미터
- 에러: `GlobalExceptionHandler` 통해 일관된 에러 형식
- 인증: Bearer JWT + X-Tenant-Id 헤더
- OpenAPI: springdoc-openapi 자동 생성

## Scope-Aware Search Notes

- 주요 문서 리스트 API는 서버에서 현재 사용자 역할의 `DataScope`를 해석해 검색 결과를 제한한다.
- 주요 문서 상세/상태변경 API도 서버에서 동일한 `DataScope`를 재검증한다.
- 주요 생성 API는 payload의 `companyCode`/`plantCode`/`departmentCode`를 같은 `DataScope`로 사전 검증한다.
- `standard-costs`와 `product-costs`는 `costCenterCode -> departmentCode`를 통해 department scope를 해석한다.
- `currency revaluations`는 `companyCode`를 통해 company scope를 해석하며 create/post/reverse에서도 같은 가드를 사용한다.
- `batch jobs`는 `companyCode`/`plantCode`/`departmentCode`를 직접 저장하고 search/get/execute/history/status/cancel/retry에서 저장된 축 중 가장 구체적인 값으로 가드를 해석한다.
- 2026-03-20 기준 적용 리소스:
  - `purchase-requests`
  - `purchase-orders`
  - `sales-orders`
  - `logistics/goods-receipts`
  - `logistics/goods-issues`
  - `production/work-orders`
  - `logistics/stock` (`PLANT`만 서버 지원)
- `ORGANIZATION`은 조직 트리에서 하위 `companyCode`/`plantCode`/`departmentCode` 집합으로 확장한다.
- `DEPARTMENT`는 명시적 `departmentCode`가 있는 리소스만 서버에서 안전하게 해석한다.
- `app:test --tests com.modularerp.security.DataScopeEnforcementTest`가 organization-scope PR 생성 허용/차단과 work-order detail/action 403을 검증한다.

## Changelog
- 2026-03-20: Initial contract registry created
- 2026-03-20: Added server-side DataScope search coverage notes
- 2026-03-20: Added targeted DataScope enforcement regression test note
- 2026-03-23: Marked notification API mismatch as VERIFIED and updated menu-profile/preference/costing coverage rows
- 2026-03-23: Added frontend API clients for contracts, quality, supply-chain, production work-centers/routings, planning schedule/capacity
- 2026-03-23: Added report/export frontend API clients and wired Excel export on the item list page
- 2026-03-23: Added sales/logistics/account/hr frontend API clients and switched existing pages to use them
- 2026-03-23: Added purchaseApi and removed unsupported PO send action from the frontend until a backend endpoint exists
- 2026-03-23: Switched budget/asset/batch/period-close/crm/currency pages from raw client calls to their existing module API clients
- 2026-03-26: Added cost-center-backed scope notes for `standard-costs` and `product-costs`
- 2026-03-26: Reworked `CostingPage` to use `costingApi`, `product-costs/calculate`, `variance`, and the current cost center / standard cost / allocation DTO fields
- 2026-03-26: Reworked `periodCloseApi`/`PeriodClosePage` to use `periods/generate`, `periods/{id}/tasks/{taskId}/execute`, and the current fiscal period / checklist / closing entry DTO fields
- 2026-03-26: Reworked `currencyApi`/`CurrencyPage` to use `/api/v1/currencies`, the current exchange-rate/revaluation DTO fields, and the new `companyCode` contract for revaluations
- 2026-03-27: Reworked `batchApi`/`BatchPage` to use `/jobs/{id}/executions`, the current batch job / execution DTO fields, and the new scope-axis contract for batch jobs
- 2026-03-27: Refined batch scope notes so organization scopes still match company-tagged jobs through the populated batch scope axis
