<script lang="ts">
  import { api, type ScoreResponseInput } from "$lib/api";
  import { app, modal, teamName } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";
  import ScoreFields from "./ScoreFields.svelte";

  const submission = app.league.scoreSubmissions.find((s) => s.id === modal.id);
  const game = app.league.games.find((g) => g.id === submission?.gameId);
  let awayScore = $state<number | null>(null);
  let homeScore = $state<number | null>(null);

  async function dispute() {
    const input: ScoreResponseInput = {
      confirm: false,
      disputedHomeScore: Number(homeScore),
      disputedAwayScore: Number(awayScore),
    };
    await api(`/scores/${submission?.id}/respond`, "POST", input);
    return "Score disputed. The commissioner can now review it.";
  }
</script>

{#if submission && game}
  <FormDialog title="Review submitted score" submitText="Dispute score" onsubmit={dispute}>
    <p class={formHelp}>
      {teamName(game.awayId)}:
      <strong>{submission.submittedAwayScore}</strong>
      –
      <strong>{submission.submittedHomeScore}</strong>
      {teamName(game.homeId)}
    </p>
    <p class={formHelp}>Confirm the score or enter the score you believe is correct.</p>
    <div class={formGrid}>
      <ScoreFields bind:awayScore bind:homeScore />
    </div>
  </FormDialog>
{/if}
