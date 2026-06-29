import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Lock, User as UserIcon, Calendar, Mail, FileText, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { DataState } from "@/components/shared/data-state";
import { PageHeader } from "@/components/shared/page-header";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Select } from "@/components/ui/select";
import { getProfile, updateProfile, updatePassword } from "../api/profile-api";
import { getApiErrorMessage } from "@/lib/http/api-error";

// Schema validate profile
const profileSchema = z.object({
  fullName: z.string().trim().min(1, "Họ tên là bắt buộc."),
  displayName: z.string().trim(),
  email: z.string().trim().min(1, "Email là bắt buộc.").email("Email không hợp lệ."),
  birthDate: z.string(),
  gender: z.enum(["", "MALE", "FEMALE", "OTHER"]),
});
type ProfileFormValues = z.infer<typeof profileSchema>;

// Schema validate password
const passwordSchema = z.object({
  currentPassword: z.string().min(1, "Vui lòng nhập mật khẩu hiện tại."),
  newPassword: z.string().min(6, "Mật khẩu mới phải từ 6 ký tự trở lên."),
  confirmPassword: z.string().min(1, "Vui lòng xác nhận mật khẩu mới."),
}).refine((data) => data.newPassword === data.confirmPassword, {
  message: "Mật khẩu mới không trùng khớp.",
  path: ["confirmPassword"],
});
type PasswordFormValues = z.infer<typeof passwordSchema>;

export function ProfilePage() {
  const [activeTab, setActiveTab] = useState<"profile" | "password">("profile");
  const [profileFeedback, setProfileFeedback] = useState("");
  const [passwordFeedback, setPasswordFeedback] = useState("");

  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey: ["userProfile"],
    queryFn: getProfile,
  });

  const profileForm = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { fullName: "", displayName: "", email: "", birthDate: "", gender: "" },
  });

  const passwordForm = useForm<PasswordFormValues>({
    resolver: zodResolver(passwordSchema),
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
  });

  // Reset form profile khi load xong data
  useEffect(() => {
    if (query.data) {
      profileForm.reset({
        fullName: query.data.fullName,
        displayName: query.data.displayName ?? "",
        email: query.data.email ?? "",
        birthDate: query.data.birthDate ?? "",
        gender: query.data.gender ?? "",
      });
    }
  }, [profileForm, query.data]);

  // Mutation cập nhật thông tin cá nhân
  const updateProfileMutation = useMutation({
    mutationFn: (values: ProfileFormValues) =>
      updateProfile({
        fullName: values.fullName,
        displayName: values.displayName || "",
        email: values.email,
        birthDate: values.birthDate || "",
        gender: values.gender || "OTHER",
      }),
    onSuccess: async () => {
      setProfileFeedback("Cập nhật thông tin cá nhân thành công.");
      await queryClient.invalidateQueries({ queryKey: ["userProfile"] });
      // Xóa thông báo sau 3 giây
      setTimeout(() => setProfileFeedback(""), 3000);
    },
  });

  // Mutation đổi mật khẩu
  const updatePasswordMutation = useMutation({
    mutationFn: (values: PasswordFormValues) =>
      updatePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      }),
    onSuccess: () => {
      setPasswordFeedback("Thay đổi mật khẩu thành công.");
      passwordForm.reset();
      // Xóa thông báo sau 3 giây
      setTimeout(() => setPasswordFeedback(""), 3000);
    },
  });

  const handleProfileSubmit = (values: ProfileFormValues) => {
    setProfileFeedback("");
    updateProfileMutation.mutate(values);
  };

  const handlePasswordSubmit = (values: PasswordFormValues) => {
    setPasswordFeedback("");
    updatePasswordMutation.mutate(values);
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title="Quản lý tài khoản"
        description="Xem, chỉnh sửa thông tin cá nhân và thay đổi mật khẩu của bạn."
      />

      <DataState
        loading={query.isLoading}
        error={query.error ? getApiErrorMessage(query.error) : null}
        empty={!query.data}
        onRetry={() => query.refetch()}
      >
        {query.data && (
          <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
            {/* Sidebar điều hướng con */}
            <div className="flex flex-row gap-2 overflow-x-auto pb-2 lg:flex-col lg:overflow-visible lg:pb-0">
              <button
                onClick={() => setActiveTab("profile")}
                className={`flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-semibold transition-all duration-200 cursor-pointer whitespace-nowrap lg:w-full ${
                  activeTab === "profile"
                    ? "bg-primary/10 text-primary"
                    : "text-muted hover:bg-primary/5 hover:text-primary"
                }`}
              >
                <UserIcon className="h-4 w-4" />
                Thông tin cá nhân
              </button>
              <button
                onClick={() => setActiveTab("password")}
                className={`flex items-center gap-3 rounded-lg px-4 py-3 text-sm font-semibold transition-all duration-200 cursor-pointer whitespace-nowrap lg:w-full ${
                  activeTab === "password"
                    ? "bg-primary/10 text-primary"
                    : "text-muted hover:bg-primary/5 hover:text-primary"
                }`}
              >
                <Lock className="h-4 w-4" />
                Thay đổi mật khẩu
              </button>
            </div>

            {/* Khung nội dung chính */}
            <div className="min-w-0">
              {activeTab === "profile" ? (
                <div className="grid gap-6 md:grid-cols-[1fr_1.8fr]">
                  {/* Cột trái: Thông tin định danh (chỉ đọc) */}
                  <div className="rounded-xl border border-line bg-surface p-5 shadow-soft">
                    <h3 className="m-0 text-lg font-bold border-b border-line pb-3">Thông tin tài khoản</h3>
                    <dl className="mt-4 space-y-4 text-sm">
                      <div>
                        <dt className="text-muted flex items-center gap-2"><UserRound className="h-4 w-4" /> Tên đăng nhập</dt>
                        <dd className="m-0 mt-1 font-semibold text-ink">{query.data.username}</dd>
                      </div>
                      <div>
                        <dt className="text-muted flex items-center gap-2"><FileText className="h-4 w-4" /> Mã tài khoản</dt>
                        <dd className="m-0 mt-1 font-semibold text-ink">
                          {query.data.teacherCode || query.data.studentCode || "Chưa thiết lập"}
                        </dd>
                      </div>
                      <div>
                        <dt className="text-muted flex items-center gap-2"><FileText className="h-4 w-4" /> Vai trò</dt>
                        <dd className="m-0 mt-1 font-semibold text-ink">
                          {query.data.userType === "TEACHER" ? "Giáo viên" : "Học sinh"}
                        </dd>
                      </div>
                    </dl>
                  </div>

                  {/* Cột phải: Form cập nhật thông tin */}
                  <form
                    onSubmit={profileForm.handleSubmit(handleProfileSubmit)}
                    className="rounded-xl border border-line bg-surface p-5 shadow-soft space-y-4"
                  >
                    <h3 className="m-0 text-lg font-bold border-b border-line pb-3">Cập nhật hồ sơ</h3>
                    <div className="grid gap-4 sm:grid-cols-2">
                      <Field label="Họ và tên" error={profileForm.formState.errors.fullName?.message}>
                        <Input placeholder="Nguyễn Văn A" {...profileForm.register("fullName")} />
                      </Field>
                      <Field label="Tên hiển thị" error={profileForm.formState.errors.displayName?.message}>
                        <Input placeholder="Thầy A / Bạn A" {...profileForm.register("displayName")} />
                      </Field>
                      <Field label="Email" error={profileForm.formState.errors.email?.message}>
                        <div className="relative">
                          <Mail className="absolute left-3 top-3 h-4 w-4 text-muted" />
                          <Input type="email" className="pl-9" placeholder="email@gmail.com" {...profileForm.register("email")} />
                        </div>
                      </Field>
                      <Field label="Ngày sinh" error={profileForm.formState.errors.birthDate?.message}>
                        <div className="relative">
                          <Calendar className="absolute left-3 top-3 h-4 w-4 text-muted" />
                          <Input type="date" className="pl-9" {...profileForm.register("birthDate")} />
                        </div>
                      </Field>
                      <Field label="Giới tính" error={profileForm.formState.errors.gender?.message}>
                        <Select className="w-full" {...profileForm.register("gender")}>
                          <option value="">Chưa chọn</option>
                          <option value="MALE">Nam</option>
                          <option value="FEMALE">Nữ</option>
                          <option value="OTHER">Khác</option>
                        </Select>
                      </Field>
                    </div>

                    {profileFeedback && (
                      <p role="status" className="rounded-lg bg-success/10 p-3 text-sm text-success font-medium">
                        {profileFeedback}
                      </p>
                    )}
                    {updateProfileMutation.error && (
                      <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger font-medium">
                        {getApiErrorMessage(updateProfileMutation.error)}
                      </p>
                    )}

                    <div className="flex justify-end pt-2 border-t border-line">
                      <Button type="submit" loading={updateProfileMutation.isPending}>
                        Lưu thay đổi
                      </Button>
                    </div>
                  </form>
                </div>
              ) : (
                /* Tab Đổi mật khẩu */
                <form
                  onSubmit={passwordForm.handleSubmit(handlePasswordSubmit)}
                  className="max-w-xl rounded-xl border border-line bg-surface p-5 shadow-soft space-y-4"
                >
                  <h3 className="m-0 text-lg font-bold border-b border-line pb-3">Đổi mật khẩu tài khoản</h3>
                  <div className="space-y-4">
                    <Field label="Mật khẩu hiện tại" error={passwordForm.formState.errors.currentPassword?.message}>
                      <div className="relative">
                        <Lock className="absolute left-3 top-3 h-4 w-4 text-muted" />
                        <Input type="password" className="pl-9" placeholder="••••••••" {...passwordForm.register("currentPassword")} />
                      </div>
                    </Field>
                    <Field label="Mật khẩu mới" error={passwordForm.formState.errors.newPassword?.message}>
                      <div className="relative">
                        <Lock className="absolute left-3 top-3 h-4 w-4 text-muted" />
                        <Input type="password" className="pl-9" placeholder="Tối thiểu 6 ký tự" {...passwordForm.register("newPassword")} />
                      </div>
                    </Field>
                    <Field label="Xác nhận mật khẩu mới" error={passwordForm.formState.errors.confirmPassword?.message}>
                      <div className="relative">
                        <Lock className="absolute left-3 top-3 h-4 w-4 text-muted" />
                        <Input type="password" className="pl-9" placeholder="Nhập lại mật khẩu mới" {...passwordForm.register("confirmPassword")} />
                      </div>
                    </Field>
                  </div>

                  {passwordFeedback && (
                    <p role="status" className="rounded-lg bg-success/10 p-3 text-sm text-success font-medium">
                      {passwordFeedback}
                    </p>
                  )}
                  {updatePasswordMutation.error && (
                    <p role="alert" className="rounded-lg bg-danger/10 p-3 text-sm text-danger font-medium">
                      {getApiErrorMessage(updatePasswordMutation.error)}
                    </p>
                  )}

                  <div className="flex justify-end pt-2 border-t border-line">
                    <Button type="submit" loading={updatePasswordMutation.isPending}>
                      Đổi mật khẩu
                    </Button>
                  </div>
                </form>
              )}
            </div>
          </div>
        )}
      </DataState>
    </div>
  );
}

function Field({ label, error, children }: { label: string; error?: string; children: React.ReactNode }) {
  return (
    <label className="block text-sm font-semibold text-ink">
      {label}
      <div className="mt-1.5">{children}</div>
      {error && <span className="mt-1 block text-xs text-danger font-medium">{error}</span>}
    </label>
  );
}
