import { Award, CalendarDays, LayoutDashboard, User, Zap, type LucideIcon } from "lucide-react";

// Dinh nghia cac phan duong dan chinh cua hoc sinh
export type StudentSection = "dashboard" | "exams" | "live-quizzes" | "results" | "profile";

export interface StudentNavigationItem {
  label: string;
  icon: LucideIcon;
  path: string;
  section: StudentSection;
}

export const studentNavigationItems: StudentNavigationItem[] = [
  { label: "Tổng quan", icon: LayoutDashboard, path: "/student/dashboard", section: "dashboard" },
  { label: "Ca thi", icon: CalendarDays, path: "/student/exams", section: "exams" },
  { label: "Quiz trực tiếp", icon: Zap, path: "/student/live-quizzes", section: "live-quizzes" },
  { label: "Kết quả", icon: Award, path: "/student/results", section: "results" },
  { label: "Trang ca nhan", icon: User, path: "/student/profile", section: "profile" },
];

// Phan nhom section de set highlight active menu
export function getStudentSection(pathname: string): StudentSection {
  if (pathname.startsWith("/student/profile")) return "profile";
  if (pathname.startsWith("/student/live-quizzes")) return "live-quizzes";
  if (pathname.startsWith("/student/exams")) return "exams";
  if (pathname.startsWith("/student/results")) return "results";
  return "dashboard";
}
