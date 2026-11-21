<script lang="ts">
  import { api } from "$lib/api";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { modal, teamName } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  let email = $state("");

  async function assign() {
    await api(`/teams/${modal.id}/manager`, "PUT", { email: email.trim() });
    return `Assigned the manager for ${teamName(modal.id)}.`;
  }
</script>

<FormDialog title="Assign team manager" submitText="Assign manager" onsubmit={assign}>
  <p class={formHelp}>Assign an existing account to manage {teamName(modal.id)}.</p>
  <div class={formGrid}>
    <Label class="col-span-full">Account email<Input type="email" bind:value={email} autocomplete="email" required /></Label>
  </div>
</FormDialog>
