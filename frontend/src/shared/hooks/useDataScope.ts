import { useQuery } from "@tanstack/react-query";
import { dataScopeApi, type ResolvedDataScope } from "../api/adminPhase4Api";
import { useAuth } from "./useAuth";

const DEFAULT_SCOPE: ResolvedDataScope = {
  type: "ALL",
  values: [],
  ownUserId: null,
  companyCodes: [],
  departmentCodes: [],
  plantCodes: [],
  denyAll: false,
};

export function useDataScope(resource: string) {
  const { roles } = useAuth();

  const query = useQuery({
    queryKey: ["data-scope", resource, roles],
    queryFn: () => dataScopeApi.resolve(roles, resource),
    enabled: roles.length > 0 && !!resource,
  });

  return {
    ...query,
    scope: query.data ?? DEFAULT_SCOPE,
  };
}
