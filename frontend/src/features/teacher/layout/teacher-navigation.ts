import { Activity, BookOpen, CalendarDays, ChartNoAxesColumn, User, type LucideIcon } from "lucide-react";

export type TeacherSection = "subjects" | "exams" | "monitoring" | "results" | "profile";

export interface TeacherNavigationItem {
  label: string;
  icon: LucideIcon;
  path: string;
  section: TeacherSection;
}

export const teacherNavigationItems: TeacherNavigationItem[] = [
  { label: "Môn học", icon: BookOpen, path: "/teacher/subjects", section: "subjects" },
  { label: "Ca thi", icon: CalendarDays, path: "/teacher/exams", section: "exams" },
  { label: "Giám sát ca thi", icon: Activity, path: "/teacher/monitoring", section: "monitoring" },
  { label: "Kết quả", icon: ChartNoAxesColumn, path: "/teacher/results", section: "results" },
  { label: "Trang cá nhân", icon: User, path: "/teacher/profile", section: "profile" },
];

export function getTeacherSection(pathname: string): TeacherSection {
  if (pathname.startsWith("/teacher/profile")) return "profile";
  if (pathname.startsWith("/teacher/monitoring")) return "monitoring";
  if (/^\/teacher\/exams\/[^/]+\/monitor(?:\/|$)/.test(pathname)) return "monitoring";
  if (/^\/teacher\/exams\/[^/]+\/results(?:\/|$)/.test(pathname)) return "results";
  if (/^\/teacher\/subjects\/[^/]+\/results(?:\/|$)/.test(pathname)) return "results";
  if (pathname.startsWith("/teacher/results")) return "results";
  if (/^\/teacher\/subjects\/[^/]+\/exams(?:\/|$)/.test(pathname)) return "exams";
  if (pathname.startsWith("/teacher/exams")) return "exams";
  return "subjects";
}
