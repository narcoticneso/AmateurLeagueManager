<script lang="ts">
  import { api } from "$lib/api";
  import { app, modal } from "$lib/league.svelte";
  import { Label } from "$lib/components/ui/label";
  import { Input } from "$lib/components/ui/input";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  const player = $derived(app.league.players.find((entry) => entry.id === modal.id));
  let email = $state("");

  async function link() {
    if (!player) return null;
    await api(`/teams/${player.teamId}/players/${player.id}/account`, "POST", { email: email.trim() });
    return `Linked ${player.name} to that account.`;
  }
</script>

{#if player}
  <FormDialog title="Link player account" submitText="Link account" onsubmit={link}>
    <p class={formHelp}>Link <strong>{player.name}</strong> to an existing league account.</p>
    <div class={formGrid}>
      <Label class="col-span-full">
        Account email
        <Input type="email" bind:value={email} autocomplete="email" required />
      </Label>
    </div>
  </FormDialog>
{/if}
