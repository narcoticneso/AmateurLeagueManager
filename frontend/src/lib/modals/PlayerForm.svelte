<script lang="ts">
  import { api, type PlayerInput } from "$lib/api";
  import SelectField from "$lib/components/SelectField.svelte";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { positions } from "$lib/format";
  import { modal, teamName } from "$lib/league.svelte";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  const teamId = modal.id;
  let name = $state("");
  let position = $state("QB");
  let number = $state<number | null>(null);

  async function add() {
    const input: PlayerInput = { name, position, number: Number(number) };
    await api(`/teams/${teamId}/players`, "POST", input);
    return "Player added to the roster.";
  }
</script>

<FormDialog title="Add to the roster" submitText="Add player" onsubmit={add}>
  <p class={formHelp}>
    {teamName(teamId)} · Jersey numbers must be unique within this team.
  </p>
  <div class={formGrid}>
    <Label class="col-span-full">Player name<Input bind:value={name} maxlength={60} required /></Label>
    <Label>Position<SelectField bind:value={position} options={positions.map((p) => ({ value: p, label: p }))} /></Label>
    <Label>Jersey number<Input type="number" min="0" max="99" bind:value={number} required /></Label>
  </div>
</FormDialog>
