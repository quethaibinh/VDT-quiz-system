import type {
  ExamDetail,
  ExamDraftInput,
  HandleViolation,
  ShowResultPolicy,
  StudentSummary,
} from "@/features/teacher/exams/model/exam-contracts";
import { fromApiDateTime, toApiDateTime } from "@/features/teacher/exams/lib/exam-date";

export interface ExamBuilderState {
  title: string;
  description: string;
  collectionId: string;
  startAtLocal: string;
  durationMinutes: number;
  joinBeforeMinutes: number;
  joinAfterMinutes: number;
  easyCount: number;
  mediumCount: number;
  hardCount: number;
  shuffleQuestions: boolean;
  shuffleOptions: boolean;
  showResultPolicy: ShowResultPolicy;
  autoSubmit: boolean;
  requireFullscreen: boolean;
  maxViolationAllowed: number;
  handleViolation: HandleViolation;
  selectedStudents: Map<string, StudentSummary>;
}

export interface CollectionCapacity {
  easy: number;
  medium: number;
  hard: number;
}

type ExamBuilderStep = 1 | 2 | 3 | 4 | 5;

export function createInitialBuilderState(): ExamBuilderState {
  return {
    title: "",
    description: "",
    collectionId: "",
    startAtLocal: "",
    durationMinutes: 60,
    joinBeforeMinutes: 10,
    joinAfterMinutes: 0,
    easyCount: 20,
    mediumCount: 20,
    hardCount: 10,
    shuffleQuestions: true,
    shuffleOptions: true,
    showResultPolicy: "AFTER_CLOSED",
    autoSubmit: true,
    requireFullscreen: true,
    maxViolationAllowed: 5,
    handleViolation: "LOCK",
    selectedStudents: new Map(),
  };
}

export function hydrateBuilderState(
  exam: ExamDetail,
  students: StudentSummary[],
): ExamBuilderState {
  return {
    title: exam.title,
    description: exam.description ?? "",
    collectionId: exam.collectionId,
    startAtLocal: fromApiDateTime(exam.startAt),
    durationMinutes: exam.durationMinutes,
    joinBeforeMinutes: exam.joinBeforeMinutes,
    joinAfterMinutes: exam.joinAfterMinutes,
    easyCount: exam.easyCount,
    mediumCount: exam.mediumCount,
    hardCount: exam.hardCount,
    shuffleQuestions: exam.shuffleQuestions,
    shuffleOptions: exam.shuffleOptions,
    showResultPolicy: exam.showResultPolicy,
    autoSubmit: exam.autoSubmit,
    requireFullscreen: exam.requireFullscreen,
    maxViolationAllowed: exam.maxViolationAllowed,
    handleViolation: exam.handleViolation,
    selectedStudents: new Map(students.map((student) => [student.id, student])),
  };
}

export function validateBuilderState(
  state: ExamBuilderState,
  capacity?: CollectionCapacity,
): string[] {
  return getValidationIssues(state, capacity).map(({ message }) => message);
}

export function validateBuilderStep(
  state: ExamBuilderState,
  step: ExamBuilderStep,
  capacity?: CollectionCapacity,
): string[] {
  return getValidationIssues(state, capacity)
    .filter((issue) => issue.step === step)
    .map(({ message }) => message);
}

function getValidationIssues(
  state: ExamBuilderState,
  capacity?: CollectionCapacity,
): { message: string; step?: ExamBuilderStep }[] {
  const total = state.easyCount + state.mediumCount + state.hardCount;
  return [
    !state.title.trim() && { message: "Vui lòng nhập tên ca thi.", step: 1 },
    !state.collectionId && { message: "Vui lòng chọn bộ câu hỏi.", step: 2 },
    !state.startAtLocal && { message: "Vui lòng chọn thời gian bắt đầu.", step: 1 },
    state.durationMinutes < 1 && { message: "Thời lượng phải lớn hơn 0.", step: 1 },
    (state.joinBeforeMinutes < 0 || state.joinAfterMinutes < 0) && {
      message: "Khoảng thời gian tham gia không được âm.",
    },
    [state.easyCount, state.mediumCount, state.hardCount].some((value) => value < 0) && {
      message: "Số lượng câu hỏi không được âm.",
    },
    total < 1 && { message: "Ca thi phải có ít nhất một câu hỏi.", step: 2 },
    state.selectedStudents.size < 1 && {
      message: "Vui lòng chọn ít nhất một học sinh.",
      step: 3,
    },
    state.selectedStudents.size > 100 && {
      message: "Mỗi ca thi chỉ hỗ trợ tối đa 100 học sinh.",
    },
    state.maxViolationAllowed < 0 && {
      message: "Ngưỡng vi phạm không được âm.",
      step: 4,
    },
    capacity && state.easyCount > capacity.easy && {
      message: "Số câu dễ vượt quá bộ câu hỏi.",
      step: 2,
    },
    capacity && state.mediumCount > capacity.medium && {
      message: "Số câu vừa vượt quá bộ câu hỏi.",
      step: 2,
    },
    capacity && state.hardCount > capacity.hard && {
      message: "Số câu khó vượt quá bộ câu hỏi.",
      step: 2,
    },
  ].filter((issue): issue is { message: string; step?: ExamBuilderStep } => Boolean(issue));
}

export function toExamDraftInput(state: ExamBuilderState): ExamDraftInput {
  return {
    title: state.title.trim(),
    description: state.description.trim(),
    collectionId: state.collectionId,
    easyCount: state.easyCount,
    mediumCount: state.mediumCount,
    hardCount: state.hardCount,
    startAt: toApiDateTime(state.startAtLocal),
    durationMinutes: state.durationMinutes,
    joinBeforeMinutes: state.joinBeforeMinutes,
    joinAfterMinutes: state.joinAfterMinutes,
    shuffleQuestions: state.shuffleQuestions,
    shuffleOptions: state.shuffleOptions,
    showResultPolicy: state.showResultPolicy,
    autoSubmit: state.autoSubmit,
    requireFullscreen: state.requireFullscreen,
    maxViolationAllowed: state.maxViolationAllowed,
    handleViolation: state.handleViolation,
  };
}

export function reconcileStudentIds(original: Iterable<string>, selected: Iterable<string>) {
  const originalIds = new Set(original);
  const selectedIds = new Set(selected);

  // Chi gui phan chenh lech de retry an toan va tranh tao audit thua o backend.
  return {
    added: [...selectedIds].filter((id) => !originalIds.has(id)),
    removed: [...originalIds].filter((id) => !selectedIds.has(id)),
  };
}

export function getExamRecoveryKey(examId: string) {
  return `sahara.quiz.exam-recovery.${examId}`;
}
