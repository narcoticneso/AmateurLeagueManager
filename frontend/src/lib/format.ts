export const date = (value: string, options: Intl.DateTimeFormatOptions = {}) =>
  new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    timeZone: "UTC",
    ...options,
  }).format(new Date(value + "Z"));

export const time = (value: string) =>
  date(value, {
    hour: "numeric",
    minute: "2-digit",
    month: undefined,
    day: undefined,
  });

const phases: Record<string, string> = {
  REGULAR: "Regular season",
  SEMIFINAL: "Semifinal",
  CHAMPIONSHIP: "Championship",
};

export const phaseName = (phase: string) => phases[phase] ?? "";

export const titleCase = (text: string) => text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();

export const positions = ["QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"];
