/* eslint-disable react-refresh/only-export-components */
import { createBrowserRouter, Navigate, Outlet } from "react-router-dom";
import { ForbiddenPage, NotFoundPage } from "@/app/error-pages";
import { AuthProvider } from "@/features/auth/auth-context";
import { AdminGuard } from "@/features/auth/components/admin-guard";
import { TeacherGuard } from "@/features/auth/components/teacher-guard";
import { LoginPage } from "@/features/auth/pages/login-page";
import { AdminDashboardPage } from "@/features/admin/dashboard/pages/admin-dashboard-page";
import { UserImportPage } from "@/features/admin/imports/pages/user-import-page";
import { AdminLayout } from "@/features/admin/layout/admin-layout";
import { AdminSubjectDetailPage } from "@/features/admin/subjects/pages/admin-subject-detail-page";
import { AdminSubjectListPage } from "@/features/admin/subjects/pages/admin-subject-list-page";
import { AdminUserDetailPage } from "@/features/admin/users/pages/admin-user-detail-page";
import { AdminUserListPage } from "@/features/admin/users/pages/admin-user-list-page";
import { CollectionDetailPage } from "@/features/teacher/collections/pages/collection-detail-page";
import { CollectionListPage } from "@/features/teacher/collections/pages/collection-list-page";
import { ExamBuilderPage } from "@/features/teacher/exams/pages/exam-builder-page";
import { ExamEditPage } from "@/features/teacher/exams/pages/exam-edit-page";
import { SubjectExamListPage } from "@/features/teacher/exams/pages/exam-list-page";
import { ExamSubjectPickerPage } from "@/features/teacher/exams/pages/exam-subject-picker-page";
import { QuestionImportPage } from "@/features/teacher/imports/pages/question-import-page";
import { TeacherLayout } from "@/features/teacher/layout/teacher-layout";
import { LiveQuizBuilderPage } from "@/features/teacher/live-quizzes/pages/live-quiz-builder-page";
import { LiveQuizDashboardPage } from "@/features/teacher/live-quizzes/pages/live-quiz-dashboard-page";
import { LiveQuizEditPage } from "@/features/teacher/live-quizzes/pages/live-quiz-edit-page";
import { LiveQuizLobbyPage } from "@/features/teacher/live-quizzes/pages/live-quiz-lobby-page";
import { LiveQuizListPage } from "@/features/teacher/live-quizzes/pages/live-quiz-list-page";
import { LiveQuizResultsPage } from "@/features/teacher/live-quizzes/pages/live-quiz-results-page";
import { LiveQuizRoomPage } from "@/features/teacher/live-quizzes/pages/live-quiz-room-page";
import { LiveQuizSubjectPickerPage } from "@/features/teacher/live-quizzes/pages/live-quiz-subject-picker-page";
import { MonitoringExamPickerPage } from "@/features/teacher/monitoring/pages/monitoring-exam-picker-page";
import { ExamMonitorPage } from "@/features/teacher/monitoring/pages/exam-monitor-page";
import { QuestionBankPage } from "@/features/teacher/questions/pages/question-bank-page";
import { ExamResultsPage } from "@/features/teacher/results/pages/exam-results-page";
import { ResultSubjectPickerPage } from "@/features/teacher/results/pages/result-subject-picker-page";
import { SubjectExamResultListPage } from "@/features/teacher/results/pages/subject-result-list-page";
import { SubjectDashboardPage } from "@/features/teacher/subjects/pages/subject-dashboard-page";
import { SubjectListPage } from "@/features/teacher/subjects/pages/subject-list-page";
import { StudentGuard } from "@/features/auth/components/student-guard";
import { StudentLayout } from "@/features/student/layout/student-layout";
import { StudentDashboardPage } from "@/features/student/dashboard/pages/student-dashboard-page";
import { StudentExamListPage } from "@/features/student/exams/pages/student-exam-list-page";
import { StudentExamLobbyPage } from "@/features/student/exams/pages/student-exam-lobby-page";
import { StudentExamRuntimePage } from "@/features/student/exams/pages/student-exam-runtime-page";
import { StudentLiveQuizJoinPage } from "@/features/student/live-quizzes/pages/student-live-quiz-join-page";
import { StudentLiveQuizLobbyPage } from "@/features/student/live-quizzes/pages/student-live-quiz-lobby-page";
import { StudentLiveQuizPlayPage } from "@/features/student/live-quizzes/pages/student-live-quiz-play-page";
import { StudentLiveQuizResultPage } from "@/features/student/live-quizzes/pages/student-live-quiz-result-page";
import { StudentResultDetailPage } from "@/features/student/results/pages/student-result-detail-page";
import { StudentResultsPage } from "@/features/student/results/pages/student-results-page";
import { ProfilePage } from "@/features/profile/pages/profile-page";

function RootProviders() {
  return <AuthProvider><Outlet /></AuthProvider>;
}

export const router = createBrowserRouter([
  {
    element: <RootProviders />,
    children: [
      { path: "/", element: <Navigate to="/login" replace /> },
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
            { path: "subjects/:subjectId/exams/:examId/edit", element: <ExamEditPage /> },
            { path: "live-quizzes", element: <LiveQuizSubjectPickerPage /> },
            { path: "subjects/:subjectId/live-quizzes", element: <LiveQuizListPage /> },
            { path: "subjects/:subjectId/live-quizzes/new", element: <LiveQuizBuilderPage /> },
            { path: "subjects/:subjectId/live-quizzes/:quizId/edit", element: <LiveQuizEditPage /> },
            { path: "live-quizzes/:roomId/lobby", element: <LiveQuizLobbyPage /> },
            { path: "live-quizzes/:roomId/dashboard", element: <LiveQuizDashboardPage /> },
            { path: "live-quizzes/:roomId/results", element: <LiveQuizResultsPage /> },
            { path: "live-quizzes/:roomId/room", element: <LiveQuizRoomPage /> },
            { path: "monitoring", element: <MonitoringExamPickerPage /> },
            { path: "exams/:examId/monitor", element: <ExamMonitorPage /> },
            { path: "results", element: <ResultSubjectPickerPage /> },
            { path: "subjects/:subjectId/results", element: <SubjectExamResultListPage /> },
            { path: "subjects/:subjectId/results/quizzes", element: <Navigate to=".." replace /> },
            { path: "subjects/:subjectId/results/exams", element: <Navigate to=".." replace /> },
            { path: "exams/:examId/results", element: <ExamResultsPage /> },
            { path: "profile", element: <ProfilePage /> },
          ],
        }],
      },
      {
        element: <AdminGuard />,
        children: [{
          path: "/admin",
          element: <AdminLayout />,
          children: [
            { index: true, element: <Navigate to="dashboard" replace /> },
            { path: "dashboard", element: <AdminDashboardPage /> },
            { path: "users", element: <AdminUserListPage /> },
            { path: "users/import", element: <UserImportPage /> },
            { path: "users/:userId", element: <AdminUserDetailPage /> },
            { path: "subjects", element: <AdminSubjectListPage /> },
            { path: "subjects/:subjectId", element: <AdminSubjectDetailPage /> },
          ],
        }],
      },
      {
        element: <StudentGuard />,
        children: [
          {
            path: "/student",
            element: <StudentLayout />,
            children: [
              { index: true, element: <Navigate to="dashboard" replace /> },
              { path: "dashboard", element: <StudentDashboardPage /> },
              { path: "exams", element: <StudentExamListPage /> },
              { path: "exams/:examId/lobby", element: <StudentExamLobbyPage /> },
              { path: "live-quizzes", element: <StudentLiveQuizJoinPage /> },
              { path: "live-quizzes/:roomId/lobby", element: <StudentLiveQuizLobbyPage /> },
              { path: "live-quizzes/:roomId/play", element: <StudentLiveQuizPlayPage /> },
              { path: "live-quizzes/:roomId/result", element: <StudentLiveQuizResultPage /> },
              { path: "results", element: <StudentResultsPage /> },
              { path: "results/:examId", element: <StudentResultDetailPage /> },
              { path: "profile", element: <ProfilePage /> },
            ],
          },
          { path: "/student/exams/:examId/session", element: <StudentExamRuntimePage /> },
        ],
      },
      { path: "*", element: <NotFoundPage /> },
    ],
  },
]);
