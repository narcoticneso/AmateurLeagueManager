<script lang="ts">
  import EmptyState from "$lib/components/EmptyState.svelte";
  import GameCard from "$lib/components/GameCard.svelte";
  import PageHeading from "$lib/components/PageHeading.svelte";
  import SelectField from "$lib/components/SelectField.svelte";
  import TeamBadge from "$lib/components/TeamBadge.svelte";
  import { Button } from "$lib/components/ui/button";
  import { positions } from "$lib/format";
  import { app, canEditRoster, filters, openModal, playerCount, record, team } from "$lib/league.svelte";
  import { numeric, panel, table, teamCell, upcomingRow } from "$lib/styles";
  import { cn } from "$lib/utils";

  let { id }: { id: string } = $props();

  const t = $derived(team(id));
  const roster = $derived(app.league.players.filter((p) => p.teamId === id && (filters.position === "ALL" || p.position === filters.position)));
  const positionOptions = [{ value: "ALL", label: "All positions" }, ...positions.map((p) => ({ value: p, label: p }))];
  const initials = (name: string) =>
    name
      .split(" ")
      .map((n) => n[0])
      .slice(0, 2)
      .join("");
</script>

{#if !t}
  <EmptyState title="Your team is waiting" description="Sign in to view your assigned team.">
    {#snippet action()}
      <Button onclick={() => openModal("login")}>Sign in</Button>
    {/snippet}
  </EmptyState>
{:else}
  <PageHeading eyebrow="TEAM CLUBHOUSE" title="The roster" description="Meet the {t.name}.">
    {#snippet action()}
      {#if canEditRoster(id)}
        <Button onclick={() => openModal("new-player", id)}>＋ Add player</Button>
      {/if}
    {/snippet}
  </PageHeading>

  <div class="mb-[22px] flex items-center gap-[22px] rounded-[9px] border border-line bg-panel p-[26px] max-sm:gap-3.5 max-sm:px-[15px] max-sm:py-5">
    <TeamBadge {id} size="large" />
    <div>
      <span class="mb-[7px] block mini-label">FALL 2026 · OAKWOOD FOOTBALL</span>
      <h2 class="max-sm:text-[24px]">{t.name}</h2>
      <p class="mt-1.5 text-xs text-muted max-sm:text-[10px]">
        Manager: {t.manager} · {playerCount(id)} players
      </p>
    </div>
    <div class="ml-auto text-right">
      <strong class="block font-display text-[40px] leading-normal font-semibold max-sm:text-[29px]">{record(id)}</strong>
      <span class="text-[10px] text-muted max-sm:text-[8px]">SEASON RECORD</span>
    </div>
  </div>

  <div class="mb-[22px] flex flex-wrap items-center gap-2.5 max-sm:gap-[7px]">
    <SelectField bind:value={filters.position} options={positionOptions} label="Filter roster by position" class="w-auto min-w-[150px] max-sm:max-w-full max-sm:min-w-0 max-sm:text-[11px]" />
    <span class="text-muted">{roster.length} players</span>
  </div>

  <section class={panel}>
    <div class="overflow-x-auto">
      <table class={table} aria-label="Team roster">
        <thead>
          <tr>
            <th>NO.</th>
            <th>PLAYER</th>
            <th>POSITION</th>
            {#if canEditRoster(id)}<th class={numeric}>MANAGE</th>{/if}
          </tr>
        </thead>
        <tbody>
          {#each roster as p (p.id)}
            <tr>
              <td class="text-muted">#{p.number}</td>
              <td>
                <span class={teamCell}>
                  <span class="grid size-8 place-items-center rounded-full bg-green-tint text-[10px] text-soft">{initials(p.name)}</span>
                  <strong>{p.name}</strong>
                </span>
              </td>
              <td>
                <span class="inline-block min-w-[38px] rounded-[4px] border border-green-edge px-[9px] py-[5px] text-center text-[10px] text-green">{p.position}</span>
              </td>
              {#if canEditRoster(id)}
                <td class={numeric}>
                  <Button variant="danger" onclick={() => openModal("remove-player", p.id)}>Remove</Button>
                </td>
              {/if}
            </tr>
          {/each}
        </tbody>
      </table>
    </div>
    {#if !roster.length}
      <div class={cn(upcomingRow, "text-muted")}>No players at this position.</div>
    {/if}
  </section>

  <div class="mt-7 mb-3 flex items-center justify-between">
    <h2 class="font-sans text-xs font-semibold">Team schedule</h2>
  </div>
  <div class="grid grid-cols-[repeat(3,minmax(0,1fr))] gap-[17px] max-lg:grid-cols-2 max-sm:grid-cols-1">
    {#each app.league.games.filter((g) => g.homeId === id || g.awayId === id) as g (g.id)}
      <GameCard game={g} full />
    {/each}
  </div>
{/if}
