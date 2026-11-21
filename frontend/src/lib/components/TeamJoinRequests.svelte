<script lang="ts">
  import { onMount } from "svelte";
  import { api, type JoinRequest } from "$lib/api";
  import SelectField from "$lib/components/SelectField.svelte";
  import { Button } from "$lib/components/ui/button";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { positions } from "$lib/format";
  import { refresh, showToast } from "$lib/league.svelte";
  import { panel } from "$lib/styles";

  let { teamId }: { teamId: string } = $props();
  let requests = $state<JoinRequest[]>([]);
  let loading = $state(true);
  let error = $state("");
  let approvingId = $state("");
  let position = $state("QB");
  let number = $state<number | null>(null);
  let saving = $state(false);
  let decisionError = $state("");

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

  function startApproval(id: string) {
    approvingId = id;
    position = "QB";
    number = null;
    decisionError = "";
  }

  async function decide(request: JoinRequest, approve: boolean) {
    saving = true;
    decisionError = "";
    try {
      await api(`/join-requests/${request.id}/decision`, "POST", approve ? { approve, position, number: Number(number) } : { approve });
      approvingId = "";
      showToast(approve ? `${request.participantName} is now on the roster.` : `Denied ${request.participantName}'s request.`);
      await Promise.all([load(), refresh()]);
    } catch (cause) {
      decisionError = cause instanceof Error ? cause.message : "Unable to save the decision.";
    } finally {
      saving = false;
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
    <p class="mb-3 text-xs text-muted">These participants are waiting to join. Approving adds them to the roster; denying marks their request as denied.</p>
    <ul class="divide-y divide-line">
      {#each requests as request (request.id)}
        <li class="py-3">
          <div class="flex flex-wrap items-center justify-between gap-3">
            <span class="min-w-0 break-words">{request.participantName}</span>
            <div class="flex flex-wrap items-center gap-2">
              <span class="text-xs text-muted">Pending · {new Date(request.requestedAt + "Z").toLocaleDateString()}</span>
              <Button size="small" onclick={() => startApproval(request.id)} disabled={saving}>Approve</Button>
              <Button variant="danger" size="small" onclick={() => void decide(request, false)} disabled={saving}>Deny</Button>
            </div>
          </div>
          {#if approvingId === request.id}
            <form
              class="mt-3 flex flex-wrap items-end gap-3"
              onsubmit={(event) => {
                event.preventDefault();
                void decide(request, true);
              }}
            >
              <Label>Position<SelectField bind:value={position} options={positions.map((p) => ({ value: p, label: p }))} /></Label>
              <Label>Jersey number<Input type="number" min="0" max="99" bind:value={number} required /></Label>
              <Button type="submit" disabled={saving}>Add to roster</Button>
              <Button variant="secondary" onclick={() => (approvingId = "")}>Cancel</Button>
            </form>
          {/if}
        </li>
      {/each}
    </ul>
    {#if decisionError}<p role="alert" class="mt-3 text-alert-text">{decisionError}</p>{/if}
  {/if}
</section>
