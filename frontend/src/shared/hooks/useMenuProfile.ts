import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { menuProfileApi } from "../api/adminApi";
import { useAuth } from "./useAuth";

export function useMenuProfile() {
  const { isAuthenticated } = useAuth();
  const query = useQuery({
    queryKey: ["menu-profile", "resolved"],
    queryFn: menuProfileApi.getResolved,
    enabled: isAuthenticated,
  });

  const visibleMenuCodes = useMemo(
    () => query.data?.menuItems.filter((item) => item.visible).map((item) => item.menuCode) ?? [],
    [query.data]
  );

  return {
    ...query,
    appliedProfiles: query.data?.appliedProfiles ?? [],
    visibleMenuCodes,
    isResolved: query.isSuccess,
    hasResolvedProfile: visibleMenuCodes.length > 0,
  };
}
