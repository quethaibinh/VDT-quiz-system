import { Award, CalendarDays, LayoutDashboard, type LucideIcon } from "lucide-react";

// Dinh nghia cac phan duong dan chinh cua hoc sinh
export type StudentSection = "dashboard" | "exams" | "results";

export interface StudentNavigationItem {
  label: string;
  icon: LucideIcon;
  path: string;
  section: StudentSection;
}

export const studentNavigationItems: StudentNavigationItem[] = [
  { label: "Tổng quan", icon: LayoutDashboard, path: "/student/dashboard", section: "dashboard" },
  { label: "Ca thi", icon: CalendarDays, path: "/student/exams", section: "exams" },
  { label: "Kết quả", icon: Award, path: "/student/results", section: "results" },
];

// Phan nhom section de set highlight active menu
export function getStudentSection(pathname: string): StudentSection {
  if (pathname.startsWith("/student/exams")) return "exams";
  if (pathname.startsWith("/student/results")) return "results";
  return "dashboard";
}
