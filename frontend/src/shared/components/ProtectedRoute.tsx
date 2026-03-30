import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import FullPageLoader from "./FullPageLoader";
import AccessDenied from "./AccessDenied";
import { useMenuProfile } from "../hooks/useMenuProfile";
import { findFirstAccessiblePath } from "../navigation";

interface ProtectedRouteProps {
  menuCode: string;
  children: ReactNode;
}

export default function ProtectedRoute({ menuCode, children }: ProtectedRouteProps) {
  const menuProfile = useMenuProfile();

  if (menuProfile.isLoading) {
    return <FullPageLoader />;
  }

  if (!menuProfile.isResolved) {
    return (
      <AccessDenied
        title="Menu Profile Error"
        description="Navigation policy could not be loaded. Access is blocked until a valid menu profile is resolved."
      />
    );
  }

  if (!menuProfile.visibleMenuCodes.includes(menuCode)) {
    const fallbackPath = findFirstAccessiblePath(menuProfile.visibleMenuCodes);
    if (fallbackPath && fallbackPath !== window.location.pathname) {
      return <Navigate to={fallbackPath} replace />;
    }
    return (
      <AccessDenied
        description="This route is not included in your assigned menu profile."
      />
    );
  }

  return <>{children}</>;
}
