<script lang="ts">
  import { onMount } from "svelte";
  import { api, type JoinRequest } from "$lib/api";
  import { Button } from "$lib/components/ui/button";
  import { panel } from "$lib/styles";

  let { teamId }: { teamId: string } = $props();
  let requests = $state<JoinRequest[]>([]);
  let loading = $state(true);
  let error = $state("");

  async function load() {
    loading = true;
    error = "";
    try {
      requests = await api<JoinRequest[]>(`/teams/${teamId}/join-requests`);
    } catch (cause) {
      error = cause instanceof Error ? cause.message : "Unable to load join requests.";
    } finally {
      loading = false;
    }
  }
  onMount(() => {
    void load();
  });
</script>

<section class={`${panel} mb-6 p-5`} aria-label="Incoming join requests">
  <div class="mb-3 flex items-center justify-between gap-3">
    <h2 class="text-xl">Join requests</h2>
    <Button variant="secondary" onclick={load} disabled={loading}>Refresh requests</Button>
  </div>
  {#if error}<p role="alert" class="text-alert-text">{error}</p>
  {:else if loading}<p role="status" class="text-muted">Loading requests…</p>
  {:else if !requests.length}<p class="text-muted">No pending join requests.</p>
  {:else}
    <p class="mb-3 text-xs text-muted">These participants are waiting to join. Approval controls are coming soon.</p>
    <ul class="divide-y divide-line">
      {#each requests as request (request.id)}
        <li class="flex flex-wrap justify-between gap-3 py-3">
          <span class="min-w-0 break-words">{request.participantName}</span>
          <span class="text-xs text-muted">Pending · {new Date(request.requestedAt + "Z").toLocaleDateString()}</span>
        </li>
      {/each}
    </ul>
  {/if}
</section>
