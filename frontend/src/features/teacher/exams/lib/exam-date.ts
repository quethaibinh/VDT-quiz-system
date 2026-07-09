const pad = (value: number) => String(value).padStart(2, "0");

export function toExamTimestamp(value: string): string {
  const match = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})$/.exec(value);
  if (!match) throw new RangeError("Invalid datetime-local value");

  const [, year, month, day, hour, minute] = match;
  const localDate = new Date(
    Number(year),
    Number(month) - 1,
    Number(day),
    Number(hour),
    Number(minute),
  );
  if (Number.isNaN(localDate.getTime())) throw new RangeError("Invalid datetime-local value");

  const offsetMinutes = -localDate.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? "+" : "-";
  const absoluteOffset = Math.abs(offsetMinutes);
  const offset = `${sign}${pad(Math.floor(absoluteOffset / 60))}:${pad(absoluteOffset % 60)}`;

  return `${year}-${month}-${day}T${hour}:${minute}:00${offset}`;
}

export function fromExamTimestamp(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) throw new RangeError("Invalid exam timestamp");

  return [
    date.getFullYear(),
    "-",
    pad(date.getMonth() + 1),
    "-",
    pad(date.getDate()),
    "T",
    pad(date.getHours()),
    ":",
    pad(date.getMinutes()),
  ].join("");
}

export const toApiDateTime = toExamTimestamp;
export const fromApiDateTime = fromExamTimestamp;
