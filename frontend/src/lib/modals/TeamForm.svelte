<script lang="ts">
  import { api, type TeamInput } from "$lib/api";
  import SelectField from "$lib/components/SelectField.svelte";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { commissioner, modal, team } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  const existing = modal.kind === "edit-team" ? team(modal.id) : undefined;

  let name = $state(existing?.name ?? "");
  let abbreviation = $state(existing?.abbreviation ?? "");
  let color = $state(existing?.color ?? "#b4ed62");
  let manager = $state(existing?.manager ?? "");
  let league = $state("oakwood");
  let season = $state("fall-2026");

  async function save() {
    const input: TeamInput = { name, abbreviation, color, manager };
    if (existing) {
      await api(`/teams/${existing.id}`, "PUT", input);
      return "Team details updated.";
    }
    await api("/teams", "POST", input);
    return commissioner() ? "Your new team is in the league." : "Your new team is in the league, and you're its manager.";
  }
</script>

<FormDialog title={existing ? "Edit team details" : "Bring a team to the league"} submitText={existing ? "Save team" : "Create team"} onsubmit={save}>
  {#if existing}
    <p class={formHelp}>Players and games stay with the team when its name changes.</p>
  {/if}
  <div class={formGrid}>
    <Label class="col-span-full">Team name<Input bind:value={name} maxlength={60} placeholder="e.g. Oakwood Owls" required /></Label>
    {#if !existing}
      <Label>League<SelectField bind:value={league} options={[{ value: "oakwood", label: "Oakwood Football" }]} /></Label>
      <Label>Season<SelectField bind:value={season} options={[{ value: "fall-2026", label: "Fall 2026" }]} /></Label>
    {/if}
    <Label>Abbreviation<Input bind:value={abbreviation} minlength={2} maxlength={3} pattern={"[A-Za-z]{2,3}"} placeholder="OO" required /></Label>
    <Label>Team color<Input type="color" bind:value={color} /></Label>
    {#if !existing && commissioner()}
      <Label class="col-span-full">Manager name<Input bind:value={manager} maxlength={60} required /></Label>
    {/if}
  </div>
</FormDialog>
