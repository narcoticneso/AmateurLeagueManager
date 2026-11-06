<script lang="ts">
  import PageHeading from "$lib/components/PageHeading.svelte";
  import TeamBadge from "$lib/components/TeamBadge.svelte";
  import { Button } from "$lib/components/ui/button";
  import { Input } from "$lib/components/ui/input";
  import { app, commissioner, openModal, playerCount, record } from "$lib/league.svelte";

  let query = $state("");

  const teams = $derived(app.league.teams.filter((t) => `${t.name} ${t.manager}`.toLowerCase().includes(query.toLowerCase())));
</script>

<PageHeading eyebrow="THE NEIGHBORHOOD LINEUP" title="Teams & rosters" description="Meet the people who make this league.">
  {#snippet action()}
    {#if commissioner()}
      <Button onclick={() => openModal("new-team")}>＋ Create team</Button>
    {/if}
  {/snippet}
</PageHeading>

<div class="mb-[22px] flex flex-wrap items-center gap-2.5 max-sm:gap-[7px]">
  <Input type="search" bind:value={query} placeholder="Search teams or managers…" aria-label="Search teams or managers" class="w-auto min-w-[150px] max-sm:max-w-full max-sm:min-w-0 max-sm:flex-1 max-sm:text-[11px]" />
</div>
<div class="grid grid-cols-3 gap-[18px] max-lg:grid-cols-2 max-sm:grid-cols-1">
  {#each teams as t (t.id)}
    <a class="block rounded-[9px] border border-line bg-panel p-6 transition-[border-color] duration-150 hover:border-green-edge max-sm:grid max-sm:grid-cols-[65px_1fr] max-sm:gap-x-[17px] max-sm:p-5" href="#team/{t.id}">
      <TeamBadge id={t.id} size="large" class="max-sm:row-span-2" />
      <h2 class="mt-[17px] max-sm:mt-[5px]">{t.name}</h2>
      <p class="mt-2 text-[11px] text-muted">Managed by {t.manager}</p>
      <div class="mt-[22px] flex items-center justify-between border-t border-line pt-[17px] text-[11px] max-sm:col-span-full">
        <span>{record(t.id)} &nbsp; · &nbsp; {playerCount(t.id)} players</span>
        <span class="text-green">View roster ↗</span>
      </div>
    </a>
  {:else}
    <p class="text-muted">No teams match your search.</p>
  {/each}
</div>
