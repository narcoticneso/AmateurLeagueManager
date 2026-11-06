import { api, useCsrfToken, type LeagueState, type SessionInfo } from "./api";

export const app = $state<{
  status: "loading" | "ready" | "failed";
  league: LeagueState;
  session: SessionInfo;
}>({
  status: "loading",
  league: {
    teams: [],
    players: [],
    games: [],
    standings: [],
    scoreSubmissions: [],
  },
  session: { username: "", view: "GUEST", teamId: "", csrf: "" },
});

export async function refresh() {
  const [league, session] = await Promise.all([api<LeagueState>("/state"), api<SessionInfo>("/session")]);
  useCsrfToken(session.csrf);
  app.league = league;
  app.session = session;
  app.status = "ready";
}

export const team = (id: string) => app.league.teams.find((t) => t.id === id);

export const teamName = (id: string) => team(id)?.name ?? "";

export const commissioner = () => app.session.view === "COMMISSIONER";

export const canEditRoster = (id: string) => commissioner() || (app.session.view === "MANAGER" && app.session.teamId === id);

export const record = (id: string) => {
  const s = app.league.standings.find((s) => s.teamId === id);
  return s ? `${s.wins}–${s.losses}–${s.ties}` : "0–0–0";
};

export const playerCount = (id: string) => app.league.players.filter((p) => p.teamId === id).length;

export const toast = $state({ message: "", visible: false });
let toastTimer: ReturnType<typeof setTimeout>;

export function showToast(message: string) {
  clearTimeout(toastTimer);
  toast.message = message;
  toast.visible = true;
  toastTimer = setTimeout(() => (toast.visible = false), 3500);
}

export type ModalKind = "login" | "new-game" | "edit-game" | "tournament-game" | "new-team" | "new-player" | "remove-player" | "submit-score" | "dispute-score" | "confirm-score" | "resolve-score";

export const modal = $state({ kind: null as ModalKind | null, id: "" });

export function openModal(kind: ModalKind, id = "") {
  modal.kind = kind;
  modal.id = id;
}

export function closeModal() {
  modal.kind = null;
}

export const filters = $state({
  schedule: "SCHEDULED",
  week: "ALL",
  position: "ALL",
});
