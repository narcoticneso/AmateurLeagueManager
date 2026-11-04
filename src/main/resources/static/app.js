"use strict";

// One small client, one Java API. The server owns permissions and saved league data.
let league = { teams: [], players: [], games: [], standings: [], scoreSubmissions: [] };
let session = { username: "", view: "GUEST", teamId: "", csrf: "" };
let scheduleFilter = "SCHEDULED",
  weekFilter = "ALL",
  positionFilter = "ALL";
let modalSubmit = null,
  toastTimer;
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) =>
  String(value ?? "").replace(
    /[&<>"']/g,
    (c) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[
        c
      ],
  );
const e = escapeHtml;
const team = (id) => league.teams.find((t) => t.id === id);
const standing = (id) => league.standings.find((s) => s.teamId === id);
const commissioner = () => session.view === "COMMISSIONER";
const canEditRoster = (id) =>
  commissioner() || (session.view === "MANAGER" && session.teamId === id);
const record = (id) => {
  const s = standing(id);
  return s ? `${s.wins}–${s.losses}–${s.ties}` : "0–0–0";
};
const date = (value, options = {}) =>
  new Intl.DateTimeFormat("en-US", {
    month: "short",
    day: "numeric",
    timeZone: "UTC",
    ...options,
  }).format(new Date(value + "Z"));
const time = (value) =>
  date(value, {
    hour: "numeric",
    minute: "2-digit",
    month: undefined,
    day: undefined,
  });
const phaseName = (phase) =>
  ({
    REGULAR: "Regular season",
    SEMIFINAL: "Semifinal",
    CHAMPIONSHIP: "Championship",
  })[phase];
const titleCase = (text) =>
  text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
const badge = (id, size = "") => {
  const t = team(id);
  return t
    ? `<span class="badge ${size}" style="--team-color:${/^#[0-9a-f]{6}$/i.test(t.color) ? t.color : "#b4ed62"}" aria-hidden="true">${e(t.abbreviation)}</span>`
    : "";
};
const status = (game) =>
  `<span class="status ${game.status === "FINAL" ? "final" : ""}">${e(game.status)}</span>`;
const icons = {
  overview:
    '<rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/>',
  schedule:
    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M7 2v6m10-6v6M3 11h18m-13 4h2m4 0h2"/>',
  teams:
    '<circle cx="9" cy="8" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3m1-16a3 3 0 0 1 0 6m2 4a5 5 0 0 1 3 5"/>',
  standings: '<path d="M4 21V12h4v9m2 0V4h4v17m2 0V8h4v13"/>',
  playoffs:
    '<path d="M7 3h10v5a5 5 0 0 1-10 0V3Zm0 2H3v3a4 4 0 0 0 4 4m10-7h4v3a4 4 0 0 1-4 4m-5 1v6m-5 2h10"/>',
  myteam: '<path d="m8 3-5 4 3 5 2-1v10h8V11l2 1 3-5-5-4a4 4 0 0 1-8 0Z"/>',
};

async function api(path, options = {}) {
  const headers = { ...options.headers };
  if (options.body && !(options.body instanceof URLSearchParams))
    headers["Content-Type"] = "application/json";
  if (options.method && options.method !== "GET")
    headers["X-CSRF-TOKEN"] = session.csrf;
  const response = await fetch("/api" + path, {
    credentials: "same-origin",
    ...options,
    headers,
  });
  if (!response.ok) {
    let detail;
    try {
      detail = await response.json();
    } catch (_) {
      /* A filter may return an empty body. */
    }
    const fallback =
      response.status === 401
        ? "Sign in with your developer username and password."
        : response.status === 403
          ? "This action is not available in your current view. Refresh if your session expired."
          : "Unable to save changes. Please try again.";
    throw new Error(detail?.message || fallback);
  }
  const text = await response.text();
  return text ? JSON.parse(text) : null;
}
async function refresh() {
  [league, session] = await Promise.all([api("/state"), api("/session")]);
  render();
}
function toast(message) {
  clearTimeout(toastTimer);
  $("#toast").textContent = message;
  $("#toast").classList.add("visible");
  toastTimer = setTimeout(() => $("#toast").classList.remove("visible"), 3500);
}
function route() {
  return (location.hash.slice(1) || "overview").split("/");
}
function heading(eyebrow, title, description, action = "") {
  return `<div class="page-heading"><div><span class="mini-label">${eyebrow}</span><h1>${title}</h1><p>${description}</p></div><div class="heading-actions">${action}</div></div>`;
}
function scheduleButton() {
  return commissioner()
    ? '<button class="button primary" data-action="new-game">＋ Schedule game</button>'
    : '<a class="button secondary" href="#schedule">View schedule ↗</a>';
}
function empty(title, description, action = "") {
  return `<div class="empty-state"><h2>${title}</h2><p>${description}</p>${action}</div>`;
}
function render() {
  const [page, id] = route();
  const navigation = [
    ["overview", "Overview"],
    ["schedule", "Schedule"],
    ["teams", "Teams & rosters"],
    ["standings", "Standings"],
    ["playoffs", "Playoffs"],
  ];
  if (session.username) navigation.push(["myteam", "My team"]);
  $("#navigation").innerHTML = navigation
    .map(
      ([key, label]) =>
        `<a class="nav-link ${page === key || (page === "team" && key === "teams") ? "active" : ""}" href="#${key}" ${page === key ? 'aria-current="page"' : ""}><svg viewBox="0 0 24 24" aria-hidden="true">${icons[key]}</svg>${label}</a>`,
    )
    .join("");
  $("#breadcrumb").textContent =
    page === "team"
      ? "Team roster"
      : navigation.find((n) => n[0] === page)?.[1] || "Overview";
  $("#account").innerHTML = session.username
    ? `<label><select class="view-select" id="view-select" aria-label="View as">${["COMMISSIONER", "MANAGER", "PLAYER"].map((v) => `<option value="${v}" ${session.view === v ? "selected" : ""}>${titleCase(v)} view</option>`).join("")}</select></label><span class="account-avatar" title="${e(titleCase(session.username))}">${e(session.username.charAt(0).toUpperCase())}</span><button class="signout" data-action="logout">Sign out</button>`
    : '<button class="button secondary small" data-action="login">Developer sign in ↗</button>';
  const pages = {
    overview: overviewPage,
    schedule: schedulePage,
    teams: teamsPage,
    standings: standingsPage,
    playoffs: playoffsPage,
    myteam: () => rosterPage(session.teamId),
    team: () => rosterPage(id),
  };
  $("#main").innerHTML = (pages[page] || overviewPage)();
  document.title = `${$("#breadcrumb").textContent} · Amateur League Manager`;
}
function standingsTable(compact = false) {
  return `<div class="table-scroll"><table class="${compact ? "overview-table" : ""}" aria-label="Regular season standings"><thead><tr><th>#</th><th>TEAM</th><th class="numeric">W–L–T</th><th class="numeric">WIN %</th><th class="numeric points-col">PF</th>${compact ? "" : '<th class="numeric">PA</th><th class="numeric">DIFF</th>'}</tr></thead><tbody>${league.standings.map((s, i) => `<tr><td class="rank">${String(i + 1).padStart(2, "0")}</td><td><a class="team-cell" href="#team/${e(s.teamId)}">${badge(s.teamId)}<span><strong>${e(team(s.teamId).name)}</strong><small>${e(team(s.teamId).manager)} · ${league.players.filter((p) => p.teamId === s.teamId).length} players</small></span></a></td><td class="numeric">${record(s.teamId)}</td><td class="numeric percentage">${s.percentage.toFixed(3)}</td><td class="numeric points-col">${s.pointsFor}</td>${compact ? "" : `<td class="numeric">${s.pointsAgainst}</td><td class="numeric">${s.pointsFor - s.pointsAgainst > 0 ? "+" : ""}${s.pointsFor - s.pointsAgainst}</td>`}</tr>`).join("")}</tbody></table></div>`;
}
function upcomingGame(game, full = false) {
  return `<article class="${full ? "game-card" : "upcoming-game"}"><div class="game-date"><span>${date(game.kickoff, { weekday: "short" })} · ${time(game.kickoff)}</span>${status(game)}</div>${[game.awayId, game.homeId].map((id, i) => `<div class="upcoming-team">${badge(id, "small")}<a href="#team/${e(id)}"><strong>${e(team(id).name)}</strong></a><span class="game-points">${game.status === "FINAL" ? (i === 0 ? game.awayScore : game.homeScore) : "—"}</span></div>`).join("")}<div class="game-location">${full ? `WEEK ${game.week} · ${phaseName(game.phase)}<br>` : ""}${e(game.location)}${full ? "<br>Away team listed first · All times Eastern" : ""}</div>${full && commissioner() ? `<button class="text-button" data-action="edit-game" data-id="${e(game.id)}">${game.status === "FINAL" ? "Edit result" : "Manage game"} ↗</button>` : ""}</article>`;
}
function overviewPage() {
  const finals = league.games.filter((g) => g.status === "FINAL");
  const recent = finals.slice(-3).reverse();
  const upcoming = league.games.filter((g) => g.status === "SCHEDULED");
  const next = upcoming[0];
  return (
    heading(
      "FALL 2026 / LEAGUE HUB",
      "Your league. All here.",
      "The matchups, the moments, and the race to the top.",
      scheduleButton(),
    ) +
    `<div class="section-label"><h2>Last time on the field</h2><span class="sample-note"><span class="green-dot"></span> Sample season · ${league.teams.length} neighborhood teams</span></div><div class="score-strip">${recent.map((g) => `<article class="result-card"><div class="result-top"><span>WEEK ${g.week} · ${phaseName(g.phase).toUpperCase()}</span><span>FINAL</span></div><div class="result-teams"><span class="result-team">${badge(g.awayId, "small")}${e(team(g.awayId).abbreviation)}</span><strong class="result-score">${g.awayScore}<span>:</span>${g.homeScore}</strong><span class="result-team">${e(team(g.homeId).abbreviation)}${badge(g.homeId, "small")}</span></div></article>`).join("")}</div>` +
    `<div class="hero-grid">${next ? `<section class="match-hero"><div class="hero-meta"><span class="live-tag"><span class="green-dot"></span> NEXT ON THE FIELD</span><span class="hero-week">WEEK ${next.week} · ${phaseName(next.phase).toUpperCase()}</span></div><div class="matchup"><div class="hero-team">${badge(next.awayId, "large")}<h3>${e(team(next.awayId).name)}</h3><div class="record">${record(next.awayId)} · AWAY</div></div><div class="versus">VS<strong>${time(next.kickoff)}</strong><small>${date(next.kickoff, { weekday: "short" }).toUpperCase()}</small></div><div class="hero-team">${badge(next.homeId, "large")}<h3>${e(team(next.homeId).name)}</h3><div class="record">${record(next.homeId)} · HOME</div></div></div><div class="hero-bottom"><span>⌖ &nbsp;${e(next.location)}</span><a class="text-button" href="#schedule">Full schedule ↗</a></div></section>` : empty("The field is clear", "Schedule the next matchup when your teams are ready.")}<section class="season-card"><span class="mini-label">SEASON AT A GLANCE <span class="green-dot"></span></span><h3>One neighborhood.<br>Plenty to play for.</h3><p>The regular season sets the stage.<br>The Oakwood Cup settles the score.</p><div class="season-stats"><div class="season-stat"><strong>${league.teams.length}</strong><span>TEAMS</span></div><div class="season-stat"><strong>${finals.filter((g) => g.phase === "REGULAR").length}</strong><span>GAMES PLAYED</span></div><div class="season-stat"><strong>01</strong><span>TROPHY</span></div></div><a class="text-button" href="#playoffs">The road to the cup ↗</a></section></div>` +
    `<div class="content-grid"><section class="panel"><div class="panel-header"><h2>League standings</h2><a class="text-button" href="#standings">Full standings ↗</a></div>${standingsTable(true)}<div class="table-footer">Regular season only · Sorted by win percentage, then point differential</div></section><section class="panel"><div class="panel-header"><h2>Coming up</h2><span class="mini-label">${upcoming.length} SCHEDULED</span></div><div class="upcoming-list">${
      upcoming
        .slice(0, 3)
        .map((g) => upcomingGame(g))
        .join("") ||
      '<div class="upcoming-game muted">No games scheduled yet.</div>'
    }</div></section></div>`
  );
}
function schedulePage() {
  let games = league.games.filter(
    (g) =>
      (scheduleFilter === "ALL" || g.status === scheduleFilter) &&
      (weekFilter === "ALL" || g.week === Number(weekFilter)),
  );
  const weeks = [...new Set(league.games.map((g) => g.week))].sort(
    (a, b) => a - b,
  );
  return (
    heading(
      "GET READY FOR GAME DAY",
      "League schedule",
      "Every matchup, from the opening kickoff to the final whistle.",
      commissioner() ? scheduleButton() : "",
    ) +
    `<div class="filters">${[
      ["ALL", "All games"],
      ["SCHEDULED", "Upcoming"],
      ["FINAL", "Results"],
      ["POSTPONED", "Postponed"],
      ["CANCELLED", "Cancelled"],
    ]
      .map(
        ([v, l]) =>
          `<button class="filter-button ${scheduleFilter === v ? "active" : ""}" data-action="schedule-filter" data-value="${v}">${l}</button>`,
      )
      .join(
        "",
      )}<select id="week-filter" aria-label="Filter by week"><option value="ALL">All weeks</option>${weeks.map((w) => `<option value="${w}" ${String(w) === weekFilter ? "selected" : ""}>Week ${w}</option>`).join("")}</select></div><div class="game-grid">${games.map((g) => upcomingGame(g, true)).join("")}</div>${games.length ? "" : empty("No games here yet", "Choose another filter or schedule a new matchup.")}`
  ) + scoreSubmissionSection(); + commissionerDisputesSection();
}
function scoreSubmissionSection() {
  if (!session.username || session.view !== "MANAGER") return "";

  const myGames = league.games.filter(
    (g) =>
      (g.homeId === session.teamId || g.awayId === session.teamId) &&
      g.status === "SCHEDULED"
  );

  const pending = league.scoreSubmissions.filter(
    (s) => s.status === "PENDING" &&
      s.submittedBy !== session.username &&
      myGames.some((g) => g.id === s.gameId)
  );

  return `
    <section class="panel" style="margin-top: 28px">
      <div class="panel-header">
        <h2>Game scores</h2>
        <span class="mini-label">MANAGER</span>
      </div>
      <p class="form-help">Submit a score after a game, or respond to an opposing manager's submission.</p>
      <div class="upcoming-list">
        ${myGames.map((g) => `
          <div class="upcoming-game">
            <strong>${e(team(g.awayId).name)} vs ${e(team(g.homeId).name)}</strong>
            <button class="button secondary small"
              data-action="submit-score" data-id="${e(g.id)}">
              Submit score
            </button>
          </div>
        `).join("")}
        ${pending.map((s) => `
          <div class="upcoming-game">
            <strong>${e(team(myGames.find((g) => g.id === s.gameId).awayId).name)}
              ${s.submittedAwayScore} – ${s.submittedHomeScore}
              ${e(team(myGames.find((g) => g.id === s.gameId).homeId).name)}</strong>
            <div>
            <button class="button primary small"
              data-action="confirm-score" data-id="${e(s.id)}">
              Confirm score
            </button>
            <button class="button secondary small"
              data-action="respond-score" data-id="${e(s.id)}">
              Dispute score
            </button>
          </div>
          </div>
        `).join("")}
      </div>
    </section>
  `;
}
function commissionerDisputesSection() {
  if (!commissioner()) return "";

  const disputed = league.scoreSubmissions.filter(
    (s) => s.status === "DISPUTED"
  );

  return `
    <section class="panel" style="margin-top: 28px">
      <div class="panel-header">
        <h2>Disputed scores</h2>
        <span class="mini-label">${disputed.length} DISPUTED</span>
      </div>
      <div class="upcoming-list">
        ${disputed.map((s) => {
          const game = league.games.find((g) => g.id === s.gameId);
          if (!game) return "";

          return `
            <div class="upcoming-game">
              <strong>
                ${e(team(game.awayId).name)}
                ${s.submittedAwayScore} – ${s.submittedHomeScore}
                ${e(team(game.homeId).name)}
              </strong>
              <p class="form-help">
                Submitted by: ${e(s.submittedBy)}
                <br>
                Opposing manager's score:
                ${s.disputedAwayScore} – ${s.disputedHomeScore}
              </p>
              <button class="button primary small"
                data-action="resolve-score" data-id="${e(s.id)}">
                Resolve dispute
              </button>
            </div>
          `;
        }).join("")}
        ${disputed.length === 0
          ? '<p class="muted">No disputed scores need attention.</p>'
          : ""}
      </div>
    </section>
  `;
}
function teamsPage() {
  return (
    heading(
      "THE NEIGHBORHOOD LINEUP",
      "Teams & rosters",
      "Meet the people who make this league.",
      commissioner()
        ? '<button class="button primary" data-action="new-team">＋ Create team</button>'
        : "",
    ) +
    `<div class="filters"><input type="search" id="team-search" placeholder="Search teams or managers…" aria-label="Search teams or managers"></div><div class="team-grid" id="team-grid">${teamCards(league.teams)}</div>`
  );
}
function teamCards(teams) {
  return (
    teams
      .map(
        (t) =>
          `<a class="team-card" href="#team/${e(t.id)}">${badge(t.id, "large")}<h2>${e(t.name)}</h2><p>Managed by ${e(t.manager)}</p><div class="team-card-bottom"><span>${record(t.id)} &nbsp; · &nbsp; ${league.players.filter((p) => p.teamId === t.id).length} players</span><span>View roster ↗</span></div></a>`,
      )
      .join("") || '<p class="muted">No teams match your search.</p>'
  );
}
function rosterPage(id) {
  const t = team(id);
  if (!t)
    return empty(
      "Your team is waiting",
      "Sign in to view your assigned team.",
      '<button class="button primary" data-action="login">Sign in</button>',
    );
  const roster = league.players.filter(
    (p) =>
      p.teamId === id &&
      (positionFilter === "ALL" || p.position === positionFilter),
  );
  return (
    heading(
      "TEAM CLUBHOUSE",
      "The roster",
      `Meet the ${e(t.name)}.`,
      canEditRoster(id)
        ? `<button class="button primary" data-action="new-player" data-id="${e(id)}">＋ Add player</button>`
        : "",
    ) +
    `<div class="roster-banner">${badge(id, "large")}<div><span class="mini-label">FALL 2026 · OAKWOOD FOOTBALL</span><h2>${e(t.name)}</h2><p>Manager: ${e(t.manager)} · ${league.players.filter((p) => p.teamId === id).length} players</p></div><div class="roster-record"><strong>${record(id)}</strong><span>SEASON RECORD</span></div></div><div class="filters"><select id="position-filter" aria-label="Filter roster by position">${["ALL", "QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"].map((p) => `<option value="${p}" ${p === positionFilter ? "selected" : ""}>${p === "ALL" ? "All positions" : p}</option>`).join("")}</select><span class="muted">${roster.length} players</span></div><section class="panel"><div class="table-scroll"><table aria-label="Team roster"><thead><tr><th>NO.</th><th>PLAYER</th><th>POSITION</th>${canEditRoster(id) ? '<th class="numeric">MANAGE</th>' : ""}</tr></thead><tbody>${roster
      .map(
        (p) =>
          `<tr><td class="jersey">#${p.number}</td><td><span class="team-cell"><span class="player-initials">${e(
            p.name
              .split(" ")
              .map((n) => n[0])
              .slice(0, 2)
              .join(""),
          )}</span><strong>${e(p.name)}</strong></span></td><td><span class="position">${e(p.position)}</span></td>${canEditRoster(id) ? `<td class="numeric"><button class="text-button danger" data-action="remove-player" data-id="${e(p.id)}" data-team="${e(id)}">Remove</button></td>` : ""}</tr>`,
      )
      .join(
        "",
      )}</tbody></table></div>${roster.length ? "" : '<div class="upcoming-game muted">No players at this position.</div>'}</section><div class="section-label" style="margin-top:28px"><h2>Team schedule</h2></div><div class="game-grid">${league.games
      .filter((g) => g.homeId === id || g.awayId === id)
      .map((g) => upcomingGame(g, true))
      .join("")}</div>`
  );
}
function standingsPage() {
  return (
    heading(
      "THE RACE TO THE TOP",
      "League standings",
      "Every game counts. Follow the regular-season table.",
    ) +
    `<section class="panel"><div class="panel-header"><h2>Fall 2026 standings</h2><span class="mini-label">${league.teams.length} TEAMS</span></div>${standingsTable()}<div class="table-footer">WIN % = (wins + ½ ties) ÷ games played. Tiebreak: point differential. Equal records and differential use a stable team-ID order, not an official playoff seed. Tournament results are excluded.</div></section>`
  );
}
function playoffsPage() {
  const semis = league.games.filter((g) => g.phase === "SEMIFINAL");
  const finals = league.games.filter((g) => g.phase === "CHAMPIONSHIP");
  return (
    heading(
      "ONE SEASON. ONE CHAMPION.",
      "The Oakwood Cup",
      "Your neighborhood’s postseason tournament.",
      commissioner()
        ? '<button class="button primary" data-action="tournament-game">＋ Add cup game</button>'
        : "",
    ) +
    `<section class="tournament-banner"><div><span class="mini-label">FALL 2026 TOURNAMENT</span><h2>The road to the cup starts here.</h2><p>Four teams. Two semifinals. One neighborhood champion.<br>The commissioner selects the teams, schedules each round, and advances the winners manually.</p></div><span class="trophy" aria-hidden="true">★</span></section><div class="bracket"><div class="bracket-column"><span class="mini-label">01 / SEMIFINALS</span>${semis.map((g) => upcomingGame(g, true)).join("") || '<div class="bracket-placeholder">Semifinal 1<br>Matchup to be scheduled</div><div class="bracket-placeholder">Semifinal 2<br>Matchup to be scheduled</div>'}</div><div class="bracket-connector"></div><div class="bracket-column"><span class="mini-label">02 / CHAMPIONSHIP</span>${finals.map((g) => upcomingGame(g, true)).join("") || '<div class="bracket-placeholder">The final matchup<br>Semifinal winners meet here. The commissioner will schedule the championship after both results are final.</div>'}</div></div>`
  );
}

function openModal(title, fields, submitText, onSubmit) {
  $("#modal-title").textContent = title;
  $("#modal-fields").innerHTML = fields;
  $("#form-error").textContent = "";
  $("#modal-submit").textContent = submitText;
  $("#modal-submit").disabled = false;
  modalSubmit = onSubmit;
  $("#modal").showModal();
}
function closeModal() {
  $("#modal").close();
}
function teamOptions(selected) {
  return league.teams
    .map(
      (t) =>
        `<option value="${e(t.id)}" ${t.id === selected ? "selected" : ""}>${e(t.name)}</option>`,
    )
    .join("");
}
function gameModal(id, tournament = false) {
  const g = league.games.find((g) => g.id === id);
  const localNow = new Date();
  localNow.setMinutes(localNow.getMinutes() - localNow.getTimezoneOffset());
  openModal(
    g
      ? "Manage matchup"
      : tournament
        ? "Schedule a cup game"
        : "Schedule a game",
    `<p class="form-help">Game times are entered in Eastern time. Choose “Final” to record a result. Changes update the schedule and standings for everyone.</p><div class="form-grid"><label>Away team<select name="awayId" required>${teamOptions(g?.awayId || league.teams[0]?.id)}</select></label><label>Home team<select name="homeId" required>${teamOptions(g?.homeId || league.teams[1]?.id)}</select></label><label class="full">Kickoff (Eastern)<input type="datetime-local" name="kickoff" value="${e(g?.kickoff || localNow.toISOString().slice(0, 16))}" required></label><label class="full">Field / location<input name="location" maxlength="100" value="${e(g?.location || "Oakwood Community Field")}" required></label><label>Week<input name="week" type="number" min="1" max="30" value="${g?.week || 6}" required></label><label>Stage<select name="phase">${["REGULAR", "SEMIFINAL", "CHAMPIONSHIP"].map((p) => `<option value="${p}" ${p === (g?.phase || (tournament ? "SEMIFINAL" : "REGULAR")) ? "selected" : ""}>${phaseName(p)}</option>`).join("")}</select></label><label class="full">Status<select name="status" id="game-status">${["SCHEDULED", "FINAL", "POSTPONED", "CANCELLED"].map((s) => `<option value="${s}" ${s === (g?.status || "SCHEDULED") ? "selected" : ""}>${titleCase(s)}</option>`).join("")}</select></label><div class="score-fields" id="score-fields" ${g?.status === "FINAL" ? "" : "hidden"}><label>Away score<input name="awayScore" type="number" min="0" max="200" value="${g?.awayScore ?? ""}"></label><label>Home score<input name="homeScore" type="number" min="0" max="200" value="${g?.homeScore ?? ""}"></label></div></div>`,
    "Save game",
    async (form) => {
      const input = Object.fromEntries(new FormData(form));
      input.week = Number(input.week);
      input.homeScore = input.status === "FINAL" ? Number(input.homeScore) : null;
      input.awayScore = input.status === "FINAL" ? Number(input.awayScore) : null;
      await api("/games" + (g ? "/" + g.id : ""), {
        method: g ? "PUT" : "POST",
        body: JSON.stringify(input),
      });
      return "Game saved. Schedule and standings updated.";
    },
  );
  syncScoreFields();
}
function submitScoreModal(gameId) {
  const game = league.games.find((g) => g.id === gameId);
  if (!game) return;

  openModal(
    "Submit game score",
    `<p class="form-help">
      ${e(team(game.awayId).name)} vs ${e(team(game.homeId).name)}.
      Enter the final scores from your game.
    </p>
    <div class="form-grid">
      <label>Away score
        <input name="awayScore" type="number" min="0" max="200" required>
      </label>
      <label>Home score
        <input name="homeScore" type="number" min="0" max="200" required>
      </label>
    </div>`,
    "Submit score",
    async (form) => {
      const input = Object.fromEntries(new FormData(form));

      await api(`/games/${gameId}/scores`, {
        method: "POST",
        body: JSON.stringify({
          homeScore: Number(input.homeScore),
          awayScore: Number(input.awayScore)
        })
      });

      return "Score submitted. Waiting for the opposing manager.";
    }
  );
}
function respondScoreModal(submissionId) {
  const submission = league.scoreSubmissions.find(
    (s) => s.id === submissionId
  );

  if (!submission) return;

  const game = league.games.find(
    (g) => g.id === submission.gameId
  );

  if (!game) return;

  openModal(
    "Review submitted score",
    `<p class="form-help">
      ${e(team(game.awayId).name)}:
      <strong>${submission.submittedAwayScore}</strong>
      –
      <strong>${submission.submittedHomeScore}</strong>
      ${e(team(game.homeId).name)}
    </p>
    <p class="form-help">
      Confirm the score or enter the score you believe is correct.
    </p>
    <div class="form-grid">
      <label>Away score
        <input name="awayScore" type="number" min="0" max="200" required>
      </label>
      <label>Home score
        <input name="homeScore" type="number" min="0" max="200" required>
      </label>
    </div>`,
    "Dispute score",
    async (form) => {
      const input = Object.fromEntries(new FormData(form));

      await api(`/scores/${submissionId}/respond`, {
        method: "POST",
        body: JSON.stringify({
          confirm: false,
          disputedHomeScore: Number(input.homeScore),
          disputedAwayScore: Number(input.awayScore)
        })
      });

      return "Score disputed. The commissioner can now review it.";
    }
  );
}
function confirmScore(submissionId) {
  openModal(
    "Confirm game score",
    `<p class="form-help">
      Are you sure the submitted score is correct?
      Confirming it will make the result official.
    </p>`,
    "Confirm score",
    async () => {
      await api(`/scores/${submissionId}/respond`, {
        method: "POST",
        body: JSON.stringify({
          confirm: true
        })
      });

      return "Score confirmed. The game is now final.";
    }
  );
}
function resolveScoreModal(submissionId) {
  const submission = league.scoreSubmissions.find(
    (s) => s.id === submissionId
  );

  if (!submission) return;

  const game = league.games.find(
    (g) => g.id === submission.gameId
  );

  if (!game) return;

  openModal(
    "Resolve disputed score",
    `<p class="form-help">
      Original submission:
      ${submission.submittedAwayScore} – ${submission.submittedHomeScore}
      <br>
      Disputed score:
      ${submission.disputedAwayScore} – ${submission.disputedHomeScore}
    </p>
    <div class="form-grid">
      <label>Away score
        <input name="awayScore" type="number" min="0" max="200" required>
      </label>
      <label>Home score
        <input name="homeScore" type="number" min="0" max="200" required>
      </label>
    </div>`,
    "Resolve dispute",
    async (form) => {
      const input = Object.fromEntries(new FormData(form));

      await api(`/scores/${submissionId}/resolve`, {
        method: "POST",
        body: JSON.stringify({
          homeScore: Number(input.homeScore),
          awayScore: Number(input.awayScore)
        })
      });

      return "Dispute resolved. The official result has been updated.";
    }
  );
}
function syncScoreFields() {
  const final = $("#game-status").value === "FINAL";
  $("#score-fields").hidden = !final;
  $("#score-fields")
    .querySelectorAll("input")
    .forEach((input) => {
      input.required = final;
      input.disabled = !final;
    });
}
function loginModal() {
  openModal(
    "Welcome to the clubhouse",
    '<p class="form-help">Developer accounts: rudra, ryan, lucas, nebojsa, and kiara. Each account can switch between commissioner, manager, and player views. Local demo password: <strong>neighborhood-demo</strong> (unless configured otherwise).</p><div class="form-grid"><label class="full">Username<input name="username" autocomplete="username" placeholder="e.g. rudra" required></label><label class="full">Password<input name="password" type="password" autocomplete="current-password" required></label></div>',
    "Sign in",
    async (form) => {
      const body = new URLSearchParams(new FormData(form));
      body.set("username", body.get("username").trim().toLowerCase());
      await api("/login", { method: "POST", body });
      return "Welcome back. Your field is ready.";
    },
  );
}

document.addEventListener("click", async (event) => {
  const button = event.target.closest("[data-action]");
  if (!button) return;
  const { action, id, value } = button.dataset;
  try {
    if (action === "login") loginModal();
    if (action === "submit-score") submitScoreModal(id);
    if (action === "respond-score") respondScoreModal(id);
    if (action === "confirm-score") confirmScore(id);
    if (action === "resolve-score") resolveScoreModal(id);
    if (action === "logout") {
      await api("/logout", { method: "POST" });
      await refresh();
      toast("Signed out. You are viewing the public league.");
    }
    if (action === "schedule-filter") {
      scheduleFilter = value;
      render();
    }
    if (
      action === "new-game" ||
      action === "edit-game" ||
      action === "tournament-game"
    )
      gameModal(id, action === "tournament-game");
    if (action === "new-team")
      openModal(
        "Bring a team to the league",
        '<div class="form-grid"><label class="full">Team name<input name="name" maxlength="60" placeholder="e.g. Oakwood Owls" required></label><label>Abbreviation<input name="abbreviation" minlength="2" maxlength="3" pattern="[A-Za-z]{2,3}" placeholder="OO" required></label><label>Team color<input type="color" name="color" value="#b4ed62"></label><label class="full">Manager name<input name="manager" maxlength="60" required></label></div>',
        "Create team",
        async (form) => {
          await api("/teams", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(new FormData(form))),
          });
          return "Your new team is in the league.";
        },
      );
    if (action === "new-player")
      openModal(
        "Add to the roster",
        `<p class="form-help">${e(team(id).name)} · Jersey numbers must be unique within this team.</p><div class="form-grid"><label class="full">Player name<input name="name" maxlength="60" required></label><label>Position<select name="position">${["QB", "RB", "WR", "TE", "OL", "DL", "LB", "CB", "S", "K"].map((p) => `<option>${p}</option>`).join("")}</select></label><label>Jersey number<input name="number" type="number" min="0" max="99" required></label></div>`,
        "Add player",
        async (form) => {
          const input = Object.fromEntries(new FormData(form));
          input.number = Number(input.number);
          await api(`/teams/${id}/players`, {
            method: "POST",
            body: JSON.stringify(input),
          });
          return "Player added to the roster.";
        },
      );
    if (action === "remove-player") {
      const p = league.players.find((p) => p.id === id);
      openModal(
        "Remove player",
        `<p class="form-help">Remove <strong>${e(p.name)}</strong> from ${e(team(p.teamId).name)}? This removes their roster entry. Completed game results stay unchanged.</p>`,
        "Remove player",
        async () => {
          await api(`/teams/${p.teamId}/players/${id}`, { method: "DELETE" });
          return "Player removed from the roster.";
        },
      );
    }
  } catch (error) {
    toast(error.message);
  }
});
document.addEventListener("change", async (event) => {
  try {
    if (event.target.id === "view-select") {
      await api("/view", {
        method: "POST",
        body: JSON.stringify({ view: event.target.value }),
      });
      await refresh();
      toast(`${titleCase(session.view)} view enabled.`);
    }
    if (event.target.id === "week-filter") {
      weekFilter = event.target.value;
      render();
    }
    if (event.target.id === "position-filter") {
      positionFilter = event.target.value;
      render();
    }
    if (event.target.id === "game-status") syncScoreFields();
  } catch (error) {
    toast(error.message);
    render();
  }
});
document.addEventListener("input", (event) => {
  if (event.target.id === "team-search") {
    const query = event.target.value.toLowerCase();
    $("#team-grid").innerHTML = teamCards(
      league.teams.filter((t) =>
        `${t.name} ${t.manager}`.toLowerCase().includes(query),
      ),
    );
  }
});
$("#modal-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  $("#modal-submit").disabled = true;
  $("#form-error").textContent = "";
  try {
    const message = await modalSubmit(event.target);
    closeModal();
    await refresh();
    toast(message);
  } catch (error) {
    $("#form-error").textContent = error.message;
  } finally {
    $("#modal-submit").disabled = false;
  }
});
$("#close-modal").addEventListener("click", closeModal);
$("#cancel-modal").addEventListener("click", closeModal);
window.addEventListener("hashchange", () => {
  positionFilter = "ALL";
  render();
  window.scrollTo(0, 0);
});
refresh().catch((error) => {
  $("#main").innerHTML =
    `<section class="error-banner"><h2>The clubhouse is temporarily unavailable.</h2><p>Check that the Java server is running, then reload this page.</p><button class="button secondary" onclick="location.reload()">Try again</button></section>`;
  console.error(error);
});
