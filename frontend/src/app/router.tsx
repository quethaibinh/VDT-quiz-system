/* eslint-disable react-refresh/only-export-components */
import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { ForbiddenPage, NotFoundPage } from "@/app/error-pages";
import { AuthProvider } from "@/features/auth/auth-context";
import { TeacherGuard } from "@/features/auth/components/teacher-guard";
import { LoginPage } from "@/features/auth/pages/login-page";
import { CollectionDetailPage } from "@/features/teacher/collections/pages/collection-detail-page";
import { CollectionListPage } from "@/features/teacher/collections/pages/collection-list-page";
import { ExamBuilderPage } from "@/features/teacher/exams/pages/exam-builder-page";
import { SubjectExamListPage } from "@/features/teacher/exams/pages/exam-list-page";
import { ExamSubjectPickerPage } from "@/features/teacher/exams/pages/exam-subject-picker-page";
import { QuestionImportPage } from "@/features/teacher/imports/pages/question-import-page";
import { TeacherLayout } from "@/features/teacher/layout/teacher-layout";
import { ExamMonitorPage } from "@/features/teacher/monitoring/pages/exam-monitor-page";
import { QuestionBankPage } from "@/features/teacher/questions/pages/question-bank-page";
import { ExamResultsPage } from "@/features/teacher/results/pages/exam-results-page";
import { ResultSubjectPickerPage } from "@/features/teacher/results/pages/result-subject-picker-page";
import { SubjectResultListPage } from "@/features/teacher/results/pages/subject-result-list-page";
import { SubjectDashboardPage } from "@/features/teacher/subjects/pages/subject-dashboard-page";
import { SubjectListPage } from "@/features/teacher/subjects/pages/subject-list-page";

function RootProviders() {
  return <AuthProvider><Outlet /></AuthProvider>;
}

export const router = createBrowserRouter([
  {
    element: <RootProviders />,
    children: [
      { path: "/", element: <Navigate to="/teacher/subjects" replace /> },
      { path: "/login", element: <LoginPage /> },
      { path: "/forbidden", element: <ForbiddenPage /> },
      {
        element: <TeacherGuard />,
        children: [{
          path: "/teacher",
          element: <TeacherLayout />,
          children: [
            { index: true, element: <Navigate to="subjects" replace /> },
            { path: "subjects", element: <SubjectListPage /> },
            { path: "subjects/:subjectId", element: <SubjectDashboardPage /> },
            { path: "subjects/:subjectId/questions", element: <QuestionBankPage /> },
            { path: "subjects/:subjectId/questions/import", element: <QuestionImportPage /> },
            { path: "subjects/:subjectId/collections", element: <CollectionListPage /> },
            { path: "subjects/:subjectId/collections/:collectionId", element: <CollectionDetailPage /> },
            { path: "exams", element: <ExamSubjectPickerPage /> },
            { path: "exams/new", element: <Navigate to="/teacher/exams" replace /> },
            { path: "subjects/:subjectId/exams", element: <SubjectExamListPage /> },
            { path: "subjects/:subjectId/exams/new", element: <ExamBuilderPage /> },
            { path: "exams/:examId/monitor", element: <ExamMonitorPage /> },
            { path: "results", element: <ResultSubjectPickerPage /> },
            { path: "subjects/:subjectId/results", element: <SubjectResultListPage /> },
            { path: "exams/:examId/results", element: <ExamResultsPage /> },
          ],
        }],
      },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
