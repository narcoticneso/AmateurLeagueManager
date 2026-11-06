<script lang="ts">
  import { api, type TeamInput } from "$lib/api";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { formGrid } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  let name = $state("");
  let abbreviation = $state("");
  let color = $state("#b4ed62");
  let manager = $state("");

  async function create() {
    const input: TeamInput = { name, abbreviation, color, manager };
    await api("/teams", "POST", input);
    return "Your new team is in the league.";
  }
</script>

<FormDialog title="Bring a team to the league" submitText="Create team" onsubmit={create}>
  <div class={formGrid}>
    <Label class="col-span-full">Team name<Input bind:value={name} maxlength={60} placeholder="e.g. Oakwood Owls" required /></Label>
    <Label>Abbreviation<Input bind:value={abbreviation} minlength={2} maxlength={3} pattern={"[A-Za-z]{2,3}"} placeholder="OO" required /></Label>
    <Label>Team color<Input type="color" bind:value={color} /></Label>
    <Label class="col-span-full">Manager name<Input bind:value={manager} maxlength={60} required /></Label>
  </div>
</FormDialog>
