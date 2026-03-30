import { useMemo } from "react";
import { useQuery } from "@tanstack/react-query";
import { menuProfileApi } from "../api/adminApi";

export function useMenuProfile() {
  const query = useQuery({
    queryKey: ["menu-profile", "resolved"],
    queryFn: menuProfileApi.getResolved,
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
