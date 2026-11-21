<script lang="ts">
  import { onMount } from "svelte";
  import { api, type JoinRequest, type Team } from "$lib/api";
  import PageHeading from "$lib/components/PageHeading.svelte";
  import SelectField from "$lib/components/SelectField.svelte";
  import { Button } from "$lib/components/ui/button";
  import { app, openModal, showToast } from "$lib/league.svelte";
  import { panel } from "$lib/styles";

  let teams = $state<Team[]>([]);
  let requests = $state<JoinRequest[]>([]);
  let teamId = $state("");
  let loading = $state(true);
  let saving = $state(false);
  let error = $state("");
  const isDemo = $derived(app.session.username !== "" && !app.session.username.includes("@"));
  const options = $derived([{ value: "", label: "Choose a team" }, ...teams.map((team) => ({ value: team.id, label: `${team.name} · ${team.manager}` }))]);
  const statusLabels: Record<string, string> = { PENDING: "Pending", APPROVED: "Approved", DENIED: "Denied" };

  async function load() {
    loading = true;
    error = "";
    try {
      [teams, requests] = await Promise.all([api<Team[]>("/join-requests/teams"), api<JoinRequest[]>("/join-requests")]);
      if (!teams.some((team) => team.id === teamId)) {
        teamId = "";
      }
    } catch (cause) {
      error = cause instanceof Error ? cause.message : "Unable to load requests.";
    } finally {
      loading = false;
    }
  }

  async function submit(event: SubmitEvent) {
    event.preventDefault();
    saving = true;
    error = "";
    try {
      await api("/join-requests", "POST", { teamId });
      teamId = "";
      showToast("Join request sent to the team's manager.");
      await load();
    } catch (cause) {
      error = cause instanceof Error ? cause.message : "Unable to send request.";
    } finally {
      saving = false;
    }
  }

  onMount(() => {
    if (app.session.username && !isDemo) {
      void load();
    } else {
      loading = false;
    }
  });
</script>

<PageHeading eyebrow="FIND YOUR TEAM" title="Join a team" description="Send a request to a team manager and follow its status here." />

{#if !app.session.username}
  <p class="mb-4 text-muted">Sign in to send a join request and see your history.</p>
  <Button onclick={() => openModal("login")}>Sign in</Button>
{:else if isDemo}
  <p class="text-muted">Join requests use verified email accounts. Sign out of this demo login, then create an account or sign in with your email.</p>
{:else}
  <section class={`${panel} mb-6 p-5`} aria-label="Request to join a team">
    <h2 class="mb-3 text-xl">Choose your team</h2>
    {#if loading}
      <p role="status" class="text-muted">Loading teams and requests…</p>
    {:else if teams.length}
      <form onsubmit={submit} class="flex flex-wrap items-center gap-3">
        <SelectField bind:value={teamId} {options} label="Team to join" class="w-full min-w-0 sm:max-w-md" />
        <Button type="submit" disabled={!teamId || saving}>{saving ? "Sending…" : "Send join request"}</Button>
      </form>
    {:else if !error}
      <p class="text-muted">No teams are available to request right now. Teams need an assigned manager, and teams you already belong to or have a pending request for are excluded.</p>
    {/if}
    {#if error}<p role="alert" class="mt-3 text-alert-text">{error}</p>{/if}
  </section>

  <section class={`${panel} p-5`} aria-label="My join requests">
    <div class="mb-4 flex items-center justify-between gap-3">
      <h2 class="text-xl">My requests</h2>
      <Button variant="secondary" onclick={load} disabled={loading || saving}>Refresh</Button>
    </div>
    {#if !loading && !requests.length && !error}
      <p class="text-muted">You haven't sent any join requests yet.</p>
    {/if}
    <ul class="divide-y divide-line">
      {#each requests as request (request.id)}
        <li class="flex flex-wrap items-center justify-between gap-3 py-4">
          <div class="min-w-0">
            <a class="font-medium break-words hover:text-green" href="#team/{request.teamId}">{request.teamName}</a>
            <p class="mt-1 text-xs text-muted">Requested {new Date(request.requestedAt + "Z").toLocaleString()}</p>
          </div>
          <span class="rounded border border-line px-3 py-1 text-xs">{statusLabels[request.status] ?? request.status}</span>
        </li>
      {/each}
    </ul>
  </section>
{/if}
