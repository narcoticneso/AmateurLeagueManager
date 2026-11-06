<script lang="ts">
  import { api, type ScoreInput } from "$lib/api";
  import { app, modal } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";
  import ScoreFields from "./ScoreFields.svelte";

  const submission = app.league.scoreSubmissions.find((s) => s.id === modal.id);
  const game = app.league.games.find((g) => g.id === submission?.gameId);
  let awayScore = $state<number | null>(null);
  let homeScore = $state<number | null>(null);

  async function resolve() {
    const input: ScoreInput = {
      homeScore: Number(homeScore),
      awayScore: Number(awayScore),
    };
    await api(`/scores/${submission?.id}/resolve`, "POST", input);
    return "Dispute resolved. The official result has been updated.";
  }
</script>

{#if submission && game}
  <FormDialog title="Resolve disputed score" submitText="Resolve dispute" onsubmit={resolve}>
    <p class={formHelp}>
      Original submission:
      {submission.submittedAwayScore} – {submission.submittedHomeScore}
      <br />
      Disputed score:
      {submission.disputedAwayScore} – {submission.disputedHomeScore}
    </p>
    <div class={formGrid}>
      <ScoreFields bind:awayScore bind:homeScore />
    </div>
  </FormDialog>
{/if}
