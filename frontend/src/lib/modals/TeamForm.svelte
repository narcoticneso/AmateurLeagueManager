<script lang="ts">
  import { api, type TeamInput } from "$lib/api";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { modal, team } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  const existing = modal.kind === "edit-team" ? team(modal.id) : undefined;

  let name = $state(existing?.name ?? "");
  let abbreviation = $state(existing?.abbreviation ?? "");
  let color = $state(existing?.color ?? "#b4ed62");
  let manager = $state(existing?.manager ?? "");

  async function save() {
    const input: TeamInput = { name, abbreviation, color, manager };
    await api(existing ? `/teams/${existing.id}` : "/teams", existing ? "PUT" : "POST", input);
    return existing ? "Team details updated." : "Your new team is in the league.";
  }
</script>

<FormDialog title={existing ? "Edit team details" : "Bring a team to the league"} submitText={existing ? "Save team" : "Create team"} onsubmit={save}>
  {#if existing}
    <p class={formHelp}>Players and games stay with the team when its name changes.</p>
  {/if}
  <div class={formGrid}>
    <Label class="col-span-full">Team name<Input bind:value={name} maxlength={60} placeholder="e.g. Oakwood Owls" required /></Label>
    <Label>Abbreviation<Input bind:value={abbreviation} minlength={2} maxlength={3} pattern={"[A-Za-z]{2,3}"} placeholder="OO" required /></Label>
    <Label>Team color<Input type="color" bind:value={color} /></Label>
    {#if !existing}
      <Label class="col-span-full">Manager name<Input bind:value={manager} maxlength={60} required /></Label>
    {/if}
  </div>
</FormDialog>
