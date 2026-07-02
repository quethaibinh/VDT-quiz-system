import { Navigate, useParams } from "react-router-dom";

export function LiveQuizRoomPage() {
  const { roomId = "" } = useParams();
  return <Navigate to={`/teacher/live-quizzes/${roomId}/lobby`} replace />;
}
