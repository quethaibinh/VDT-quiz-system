import { keepPreviousData, queryOptions } from "@tanstack/react-query";
import { getAdminUser, getAdminUserStatistics, listAdminUsers } from "@/features/admin/users/api/admin-user-api";
import type { AdminUserFilters } from "@/features/admin/users/model/admin-user-contracts";

export const adminUserKeys = {
  all: ["admin", "users"] as const,
  lists: () => [...adminUserKeys.all, "list"] as const,
  list: (filters: AdminUserFilters) => [...adminUserKeys.lists(), filters] as const,
  detail: (id: string) => [...adminUserKeys.all, "detail", id] as const,
  statistics: () => [...adminUserKeys.all, "statistics"] as const,
};

export const adminUserListQuery = (filters: AdminUserFilters) => queryOptions({
  queryKey: adminUserKeys.list(filters),
  queryFn: () => listAdminUsers(filters),
  placeholderData: keepPreviousData,
});

export const adminUserDetailQuery = (id: string) => queryOptions({
  queryKey: adminUserKeys.detail(id),
  queryFn: () => getAdminUser(id),
  enabled: Boolean(id),
});

export const adminUserStatisticsQuery = () => queryOptions({
  queryKey: adminUserKeys.statistics(),
  queryFn: getAdminUserStatistics,
});
