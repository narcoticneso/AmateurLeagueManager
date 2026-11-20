<script lang="ts">
  import { modal } from "$lib/league.svelte";
  import ConfirmScoreForm from "./ConfirmScoreForm.svelte";
  import AssignTeamManagerForm from "./AssignTeamManagerForm.svelte";
  import DisputeScoreForm from "./DisputeScoreForm.svelte";
  import GameForm from "./GameForm.svelte";
  import LoginForm from "./LoginForm.svelte";
  import LinkPlayerAccountForm from "./LinkPlayerAccountForm.svelte";
  import PlayerForm from "./PlayerForm.svelte";
  import RemovePlayerForm from "./RemovePlayerForm.svelte";
  import ResolveScoreForm from "./ResolveScoreForm.svelte";
  import SubmitScoreForm from "./SubmitScoreForm.svelte";
  import TeamForm from "./TeamForm.svelte";
</script>

{#if modal.kind === "login"}
  <LoginForm />
{:else if modal.kind === "new-game" || modal.kind === "edit-game" || modal.kind === "tournament-game"}
  <GameForm />
{:else if modal.kind === "new-team"}
  <TeamForm />
{:else if modal.kind === "new-player"}
  <PlayerForm />
{:else if modal.kind === "remove-player"}
  <RemovePlayerForm />
{:else if modal.kind === "link-player-account"}
  <LinkPlayerAccountForm />
{:else if modal.kind === "assign-team-manager"}
  <AssignTeamManagerForm />
{:else if modal.kind === "submit-score"}
  <SubmitScoreForm />
{:else if modal.kind === "dispute-score"}
  <DisputeScoreForm />
{:else if modal.kind === "confirm-score"}
  <ConfirmScoreForm />
{:else if modal.kind === "resolve-score"}
  <ResolveScoreForm />
{/if}
