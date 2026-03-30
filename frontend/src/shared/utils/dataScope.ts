import type { ResolvedDataScope } from "../api/adminPhase4Api";

interface ScopeFieldAccessors<T> {
  getOwnerId?: (row: T) => string | null | undefined;
  getCompanyCode?: (row: T) => string | null | undefined;
  getOrganizationCode?: (row: T) => string | null | undefined;
  getDepartmentCode?: (row: T) => string | null | undefined;
  getPlantCode?: (row: T) => string | null | undefined;
}

interface ScopeMatchOptions<T> extends ScopeFieldAccessors<T> {
  userId: string | null;
}

export function canAccessRowByDataScope<T>(
  row: T,
  scope: ResolvedDataScope,
  { userId, getOwnerId, getCompanyCode, getOrganizationCode, getDepartmentCode, getPlantCode }: ScopeMatchOptions<T>
) {
  if (scope.denyAll) return false;
  if (scope.type === "ALL") return true;

  if (scope.ownUserId && getOwnerId && getOwnerId(row) !== scope.ownUserId) {
    return false;
  }

  if (scope.companyCodes?.length && !scope.companyCodes.includes(getCompanyCode?.(row) ?? "")) {
    return false;
  }

  if (scope.departmentCodes?.length && !scope.departmentCodes.includes(getDepartmentCode?.(row) ?? "")) {
    return false;
  }

  if (scope.plantCodes?.length && !scope.plantCodes.includes(getPlantCode?.(row) ?? "")) {
    return false;
  }

  if (scope.ownUserId || scope.companyCodes?.length || scope.departmentCodes?.length || scope.plantCodes?.length) {
    return true;
  }

  if (scope.type === "OWN") {
    return !!userId && getOwnerId?.(row) === userId;
  }

  if (scope.type === "ORGANIZATION") {
    // Business documents currently expose companyCode instead of a standalone organization code.
    return scope.values.includes(getOrganizationCode?.(row) ?? getCompanyCode?.(row) ?? "");
  }

  if (scope.type === "DEPARTMENT") {
    return scope.values.includes(getDepartmentCode?.(row) ?? "");
  }

  if (scope.type === "PLANT") {
    return scope.values.includes(getPlantCode?.(row) ?? "");
  }

  return false;
}

export function filterRowsByDataScope<T>(
  rows: T[],
  scope: ResolvedDataScope,
  options: ScopeMatchOptions<T>
) {
  // Keep unknown or partially-modeled scopes fail-closed until every resource exposes the right identifiers.
  return rows.filter((row) => canAccessRowByDataScope(row, scope, options));
}
