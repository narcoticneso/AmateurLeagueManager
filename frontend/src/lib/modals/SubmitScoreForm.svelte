<script lang="ts">
  import { api, type ScoreInput } from "$lib/api";
  import { app, modal, teamName } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";
  import ScoreFields from "./ScoreFields.svelte";

  const game = app.league.games.find((g) => g.id === modal.id);
  let awayScore = $state<number | null>(null);
  let homeScore = $state<number | null>(null);

  async function submit() {
    const input: ScoreInput = {
      homeScore: Number(homeScore),
      awayScore: Number(awayScore),
    };
    await api(`/games/${game?.id}/scores`, "POST", input);
    return "Score submitted. Waiting for the opposing manager.";
  }
</script>

{#if game}
  <FormDialog title="Submit game score" submitText="Submit score" onsubmit={submit}>
    <p class={formHelp}>
      {teamName(game.awayId)} vs {teamName(game.homeId)}. Enter the final scores from your game.
    </p>
    <div class={formGrid}>
      <ScoreFields bind:awayScore bind:homeScore />
    </div>
  </FormDialog>
{/if}
