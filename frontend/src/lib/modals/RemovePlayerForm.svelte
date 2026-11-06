<script lang="ts">
  import { api } from "$lib/api";
  import { app, modal, teamName } from "$lib/league.svelte";
  import { formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  const player = app.league.players.find((p) => p.id === modal.id);

  async function remove() {
    await api(`/teams/${player?.teamId}/players/${player?.id}`, "DELETE");
    return "Player removed from the roster.";
  }
</script>

{#if player}
  <FormDialog title="Remove player" submitText="Remove player" onsubmit={remove}>
    <p class={formHelp}>
      Remove <strong>{player.name}</strong> from {teamName(player.teamId)}? This removes their roster entry. Completed game results stay unchanged.
    </p>
  </FormDialog>
{/if}
