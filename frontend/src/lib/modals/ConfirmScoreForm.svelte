<script lang="ts">
  import { api, type ScoreResponseInput } from "$lib/api";
  import { modal } from "$lib/league.svelte";
  import { formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  const submissionId = modal.id;

  async function confirm() {
    const input: ScoreResponseInput = {
      confirm: true,
      disputedHomeScore: null,
      disputedAwayScore: null,
    };
    await api(`/scores/${submissionId}/respond`, "POST", input);
    return "Score confirmed. The game is now final.";
  }
</script>

<FormDialog title="Confirm game score" submitText="Confirm score" onsubmit={confirm}>
  <p class={formHelp}>Are you sure the submitted score is correct? Confirming it will make the result official.</p>
</FormDialog>
