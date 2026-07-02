import { useMutation } from "@tanstack/react-query";
import { LogIn, Zap } from "lucide-react";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { joinStudentLiveQuiz } from "@/features/student/live-quizzes";
import { getApiErrorMessage } from "@/lib/http/api-error";

export function StudentLiveQuizJoinPage() {
  const [code, setCode] = useState("");
  const navigate = useNavigate();
  const join = useMutation({
    mutationFn: () => joinStudentLiveQuiz({ code: code.trim().toUpperCase() }),
    onSuccess: (response) => {
      window.sessionStorage.setItem("live-quiz:last-room", response.roomId);
      const target = response.roomStatus === "STARTED" ? "play" : "lobby";
      navigate(`/student/live-quizzes/${response.roomId}/${target}`, { replace: true });
    },
  });

  const normalized = code.trim().toUpperCase();

  return (
    <div className="mx-auto max-w-3xl space-y-6">
      <PageHeader
        title="Vao quiz truc tiep"
        description="Nhap ma phong giao vien dang hien thi de vao phong cho."
      />
      <section className="rounded-xl border border-line bg-surface p-6 text-center shadow-soft md:p-8">
        <div className="mx-auto grid h-14 w-14 place-items-center rounded-full bg-primary/10 text-primary">
          <Zap size={28} />
        </div>
        <label htmlFor="live-quiz-code" className="mt-6 block text-sm font-bold uppercase tracking-wide text-muted">
          Ma phong
        </label>
        <input
          id="live-quiz-code"
          value={code}
          onChange={(event) => setCode(event.target.value.toUpperCase().replace(/\s/g, "").slice(0, 6))}
          onKeyDown={(event) => {
            if (event.key === "Enter" && normalized.length >= 4) join.mutate();
          }}
          className="mx-auto mt-3 block w-full max-w-sm rounded-lg border border-line bg-canvas px-4 py-4 text-center font-mono text-4xl font-black tracking-[0.18em] text-ink outline-none transition focus:border-primary"
          placeholder="ABC123"
          autoComplete="off"
        />
        <Button className="mt-5" loading={join.isPending} disabled={normalized.length < 4} onClick={() => join.mutate()}>
          <LogIn size={16} />
          Vao phong
        </Button>
        {join.error && (
          <p role="alert" className="mx-auto mt-4 max-w-sm rounded-lg bg-danger/10 p-3 text-sm text-danger">
            {getApiErrorMessage(join.error)}
          </p>
        )}
      </section>
    </div>
  );
}
