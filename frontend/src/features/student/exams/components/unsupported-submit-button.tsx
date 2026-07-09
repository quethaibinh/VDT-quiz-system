import { Button } from "@/components/ui/button";

interface SubmitButtonProps {
  disabled?: boolean;
  pending?: boolean;
  submitted?: boolean;
  onSubmit: () => void;
}

export function SubmitButton({ disabled, pending, submitted, onSubmit }: SubmitButtonProps) {
  if (submitted) {
    return (
      <Button type="button" variant="secondary" disabled className="text-success">
        Đã nộp bài
      </Button>
    );
  }

  return (
    <Button type="button" variant="primary" loading={pending} disabled={disabled} onClick={onSubmit}>
      Nộp bài
    </Button>
  );
}
