import { useQuery } from '@tanstack/react-query';
import { useAuth } from './useAuth';
import api, { type ApiResponse } from '../api/client';

export type FieldAccessLevel = 'FULL' | 'READONLY' | 'MASKED' | 'HIDDEN';

interface FieldPermissionMap {
  [fieldName: string]: FieldAccessLevel;
}

/**
 * 현재 사용자의 역할에 대한 필드 레벨 권한을 가져오는 훅.
 * 반환값으로 필드별 접근 수준을 확인할 수 있음.
 */
export function useFieldPermission(resource: string) {
  const { roles } = useAuth();
  const { data: permissions = {}, isLoading: loading } = useQuery({
    queryKey: ['field-permissions', roles, resource],
    queryFn: async () => {
      const response = await api.get<ApiResponse<Record<string, string>>>('/api/v1/admin/field-permissions/merged', {
        params: { roles: roles.join(','), resource },
      });
      return (response.data.data ?? {}) as FieldPermissionMap;
    },
    enabled: roles.length > 0 && !!resource,
  });

  const getAccess = (fieldName: string): FieldAccessLevel =>
    permissions[fieldName] ?? 'FULL';

  const isVisible = (fieldName: string) => getAccess(fieldName) !== 'HIDDEN';
  const isReadonly = (fieldName: string) => {
    const level = getAccess(fieldName);
    return level === 'READONLY' || level === 'MASKED';
  };
  const isMasked = (fieldName: string) => getAccess(fieldName) === 'MASKED';

  return { permissions, loading, getAccess, isVisible, isReadonly, isMasked };
}
