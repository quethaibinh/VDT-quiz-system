import { queryOptions } from "@tanstack/react-query";
import { getSubject, listSubjects } from "@/features/teacher/subjects/api/subject-api";

export const subjectKeys = {
  all: ["teacher", "subjects"] as const,
  list: (keyword: string) => [...subjectKeys.all, "list", keyword] as const,
  detail: (id: string) => [...subjectKeys.all, "detail", id] as const,
};

export const subjectListQuery = (keyword = "") => queryOptions({
  queryKey: subjectKeys.list(keyword),
  queryFn: () => listSubjects({ status: "ACTIVE", keyword: keyword || undefined }),
});

export const subjectDetailQuery = (id: string) => queryOptions({
  queryKey: subjectKeys.detail(id),
  queryFn: () => getSubject(id),
  enabled: Boolean(id),
});
