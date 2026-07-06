export interface StudentSubjectItem {
  subjectId: string | null;
  subjectName: string | null;
}

export interface StudentSubjectGroup {
  subjectId: string;
  subjectName: string;
  total: number;
  isFallback: boolean;
}

export function buildStudentSubjectGroups<T extends StudentSubjectItem>(items: T[]): StudentSubjectGroup[] {
  const groups = new Map<string, StudentSubjectGroup>();

  for (const item of items) {
    const subjectName = item.subjectName?.trim() || "Chưa rõ môn học";
    const subjectId = item.subjectId?.trim() || `unknown:${subjectName.toLowerCase()}`;
    const current = groups.get(subjectId);

    if (current) {
      current.total += 1;
      continue;
    }

    groups.set(subjectId, {
      subjectId,
      subjectName,
      total: 1,
      isFallback: !item.subjectId,
    });
  }

  return Array.from(groups.values()).sort((left, right) => left.subjectName.localeCompare(right.subjectName, "vi"));
}
