import { zodResolver } from "@hookform/resolvers/zod";
import { LockKeyhole, UserRound } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { Navigate, useNavigate, useSearchParams } from "react-router-dom";
import { z } from "zod";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { login } from "@/features/auth/api/auth-api";
import { useAuth } from "@/features/auth/auth-context";
import { getApiErrorMessage } from "@/lib/http/api-error";

const schema = z.object({
  username: z.string().min(1, "Vui lòng nhập tên đăng nhập."),
  password: z.string().min(1, "Vui lòng nhập mật khẩu."),
});
type FormValue = z.infer<typeof schema>;

export function LoginPage() {
  const { session, signIn } = useAuth();
  const navigate = useNavigate();
  const [search] = useSearchParams();
  const [serverError, setServerError] = useState("");
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormValue>({ resolver: zodResolver(schema) });

  if (session?.claims.userRole === "TEACHER") return <Navigate to="/teacher/subjects" replace />;

  async function submit(values: FormValue) {
    setServerError("");
    try {
      const token = await login(values);
      signIn(token);
      const returnTo = search.get("returnTo");
      navigate(returnTo?.startsWith("/") ? returnTo : "/teacher/subjects", { replace: true });
    } catch (error) {
      setServerError(getApiErrorMessage(error));
    }
  }

  return (
    <main className="grid min-h-screen bg-canvas lg:grid-cols-[1.1fr_0.9fr]">
      <section className="hidden flex-col justify-between p-16 lg:flex">
        <strong className="font-serif text-2xl tracking-[0.16em] text-primary">SAHARA QUIZ</strong>
        <div className="max-w-2xl">
          <h1 className="text-6xl leading-[0.95]">Không gian thi cử gọn gàng cho thầy và trò.</h1>
          <p className="mt-8 max-w-lg text-lg leading-8 text-muted">Tạo bộ câu hỏi, tổ chức ca thi và theo dõi kết quả trong một không gian tập trung.</p>
        </div>
        <p className="text-sm text-muted">Nền tảng thi trắc nghiệm trực tuyến</p>
      </section>
      <section className="grid place-items-center p-6">
        <form onSubmit={handleSubmit(submit)} className="w-full max-w-md rounded-2xl border border-line bg-surface p-7 shadow-soft md:p-10">
          <h2 className="m-0 text-4xl">Chào mừng trở lại</h2>
          <p className="mt-2 text-sm text-muted">Đăng nhập bằng tài khoản giáo viên.</p>
          <label className="mt-8 block text-sm font-semibold">Tên đăng nhập</label>
          <div className="relative mt-2"><UserRound className="absolute left-3 top-3 h-5 w-5 text-muted" /><Input className="pl-10" autoComplete="username" {...register("username")} /></div>
          {errors.username && <p className="text-sm text-danger">{errors.username.message}</p>}
          <label className="mt-5 block text-sm font-semibold">Mật khẩu</label>
          <div className="relative mt-2"><LockKeyhole className="absolute left-3 top-3 h-5 w-5 text-muted" /><Input type="password" className="pl-10" autoComplete="current-password" {...register("password")} /></div>
          {errors.password && <p className="text-sm text-danger">{errors.password.message}</p>}
          {serverError && <p role="alert" className="mt-4 rounded-lg bg-danger/10 p-3 text-sm text-danger">{serverError}</p>}
          <Button type="submit" loading={isSubmitting} className="mt-7 w-full">Đăng nhập</Button>
        </form>
      </section>
    </main>
  );
}
