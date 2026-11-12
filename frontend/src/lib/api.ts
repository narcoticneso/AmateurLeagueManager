import type { components } from "./api-schema";

type Schemas = components["schemas"];
export type Team = Schemas["Team"];
export type Player = Schemas["Player"];
export type Game = Schemas["Game"];
export type Standing = Schemas["Standing"];
export type ScoreSubmission = Schemas["ScoreSubmission"];
export type LeagueState = Schemas["LeagueState"];
export type SessionInfo = Schemas["SessionInfo"];
export type GameInput = Schemas["GameInput"];
export type TeamInput = Schemas["TeamInput"];
export type PlayerInput = Schemas["PlayerInput"];
export type ScoreInput = Schemas["ScoreInput"];
export type ScoreResponseInput = Schemas["ScoreResponseInput"];
export type ViewInput = Schemas["ViewInput"];

let csrfToken = "";

export function useCsrfToken(token: string) {
  csrfToken = token;
}

export async function api<T = null>(path: string, method = "GET", body?: object | URLSearchParams | FormData): Promise<T> {
  const headers: Record<string, string> = {};
    if (
        body &&
        !(body instanceof URLSearchParams) &&
        !(body instanceof FormData)
    ) {
        headers["Content-Type"] = "application/json";
    }
  if (method !== "GET") {
    headers["X-CSRF-TOKEN"] = csrfToken;
  }
  const response = await fetch("/api" + path, {
    method,
    credentials: "same-origin",
    headers,
      body:
          body instanceof URLSearchParams || body instanceof FormData
              ? body
              : body && JSON.stringify(body),
  });
  if (!response.ok) {
    const detail = (await response.json().catch(() => null)) as {
      message?: string;
    } | null;
    const fallback =
      response.status === 401 ? "Sign in with your developer username and password." : response.status === 403 ? "This action is not available in your current view. Refresh if your session expired." : "Unable to save changes. Please try again.";
    throw new Error(detail?.message || fallback);
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : null) as T;
}

export async function apiBlob(path: string): Promise<Blob | null> {
    const response = await fetch("/api" + path, {
        method: "GET",
        credentials: "same-origin",
    });

    if (response.status === 404) {
        return null;
    }

    if (!response.ok) {
        throw new Error("Unable to load profile picture.");
    }

    return await response.blob();
}
