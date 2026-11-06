<script lang="ts">
  import { api, type GameInput } from "$lib/api";
  import SelectField from "$lib/components/SelectField.svelte";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { phaseName, titleCase } from "$lib/format";
  import { app, modal } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";
  import ScoreFields from "./ScoreFields.svelte";

  const game = app.league.games.find((g) => g.id === modal.id);
  const tournament = modal.kind === "tournament-game";
  const localNow = new Date(Date.now() - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16);

  let awayId = $state(game?.awayId || app.league.teams[0]?.id || "");
  let homeId = $state(game?.homeId || app.league.teams[1]?.id || "");
  let kickoff = $state(game?.kickoff || localNow);
  let location = $state(game?.location || "Oakwood Community Field");
  let week = $state(game?.week || 6);
  let phase = $state(game?.phase || (tournament ? "SEMIFINAL" : "REGULAR"));
  let status = $state(game?.status || "SCHEDULED");
  let awayScore = $state(game?.awayScore ?? null);
  let homeScore = $state(game?.homeScore ?? null);

  const final = $derived(status === "FINAL");
  const teamOptions = app.league.teams.map((t) => ({
    value: t.id,
    label: t.name,
  }));
  const phaseOptions = ["REGULAR", "SEMIFINAL", "CHAMPIONSHIP"].map((p) => ({
    value: p,
    label: phaseName(p),
  }));
  const statusOptions = ["SCHEDULED", "FINAL", "POSTPONED", "CANCELLED"].map((s) => ({ value: s, label: titleCase(s) }));

  async function save() {
    const input: GameInput = {
      homeId,
      awayId,
      kickoff,
      location,
      week,
      phase,
      status,
      homeScore: final ? Number(homeScore) : null,
      awayScore: final ? Number(awayScore) : null,
    };
    await api(game ? `/games/${game.id}` : "/games", game ? "PUT" : "POST", input);
    return "Game saved. Schedule and standings updated.";
  }
</script>

<FormDialog title={game ? "Manage matchup" : tournament ? "Schedule a cup game" : "Schedule a game"} submitText="Save game" onsubmit={save}>
  <p class={formHelp}>Game times are entered in Eastern time. Choose “Final” to record a result. Changes update the schedule and standings for everyone.</p>
  <div class={formGrid}>
    <Label>Away team<SelectField bind:value={awayId} options={teamOptions} /></Label>
    <Label>Home team<SelectField bind:value={homeId} options={teamOptions} /></Label>
    <Label class="col-span-full">Kickoff (Eastern)<Input type="datetime-local" bind:value={kickoff} required /></Label>
    <Label class="col-span-full">Field / location<Input maxlength={100} bind:value={location} required /></Label>
    <Label>Week<Input type="number" min="1" max="30" bind:value={week} required /></Label>
    <Label>Stage<SelectField bind:value={phase} options={phaseOptions} /></Label>
    <Label class="col-span-full">Status<SelectField bind:value={status} options={statusOptions} /></Label>
    <div class="col-span-full grid grid-cols-2 gap-4" hidden={!final}>
      <ScoreFields bind:awayScore bind:homeScore disabled={!final} />
    </div>
  </div>
</FormDialog>
