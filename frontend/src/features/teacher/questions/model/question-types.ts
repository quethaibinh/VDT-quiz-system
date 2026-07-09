export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type Visibility = "PUBLIC" | "PRIVATE";

export interface Question {
  id: string;
  subjectId: string;
  topicId?: string | null;
  ownerTeacherId: string;
  questionType: "SINGLE_CHOICE" | "MULTI_CHOICE";
  content: string;
  difficulty: Difficulty;
  defaultScore: number;
  estimatedSecond: number;
  visibility: Visibility;
  status: "ACTIVE" | "ARCHIVED";
  createdAt: string;
  updatedAt: string;
}

export interface QuestionOption {
  id: string;
  optionKey: string;
  content: string;
  contentFormat?: string | null;
  explanation?: string | null;
  correct?: boolean | null;
}

export interface QuestionDetail extends Question {
  contentFormat?: string | null;
  explanation?: string | null;
  source: "MANUAL" | "EXCEL";
  options: QuestionOption[];
}

export interface QuestionOptionInput {
  optionKey: string;
  content: string;
  contentFormat?: string | null;
  explanation?: string | null;
  correct: boolean;
}

export interface QuestionInput {
  topicId: string;
  questionType: "SINGLE_CHOICE" | "MULTI_CHOICE";
  content: string;
  contentFormat?: string | null;
  explanation?: string | null;
  difficulty: Difficulty;
  defaultScore?: number | null;
  estimatedSecond?: number | null;
  visibility?: Visibility | null;
  options: QuestionOptionInput[];
}

export interface Topic {
  id: string;
  subjectId: string;
  name: string;
  description?: string | null;
  slug: string;
  parentTopicId?: string | null;
}

export interface QuestionFilters {
  keyword?: string;
  topicId?: string;
  difficulty?: string;
  visibility?: string;
  ownerScope?: string;
  questionType?: string;
  collectionId?: string;
  membership?: string;
  page: number;
  size: number;
  sort: string;
}
