<script lang="ts">
  import GameCard from "$lib/components/GameCard.svelte";
  import PageHeading from "$lib/components/PageHeading.svelte";
  import { Button } from "$lib/components/ui/button";
  import { app, commissioner, openModal } from "$lib/league.svelte";

  const semis = $derived(app.league.games.filter((g) => g.phase === "SEMIFINAL"));
  const finals = $derived(app.league.games.filter((g) => g.phase === "CHAMPIONSHIP"));
  const column = "grid gap-[19px] [&>.mini-label]:px-[3px]";
  const placeholder = "rounded-lg border border-dashed border-edge bg-sunken p-[30px] text-xs leading-[1.8] text-muted";
</script>

<PageHeading eyebrow="ONE SEASON. ONE CHAMPION." title="The Oakwood Cup" description="Your neighborhood’s postseason tournament.">
  {#snippet action()}
    {#if commissioner()}
      <Button onclick={() => openModal("tournament-game")}>＋ Add cup game</Button>
    {/if}
  {/snippet}
</PageHeading>

<section class="mb-[26px] flex items-center justify-between gap-5 rounded-lg border border-edge bg-linear-[110deg] from-hero-from to-hero-to p-[25px] max-sm:p-5">
  <div>
    <span class="mini-label">FALL 2026 TOURNAMENT</span>
    <h2 class="my-2 text-[31px]">The road to the cup starts here.</h2>
    <p class="text-xs leading-[1.7] text-muted max-sm:text-[11px]">
      Four teams. Two semifinals. One neighborhood champion.<br />The commissioner selects the teams, schedules each round, and advances the winners manually.
    </p>
  </div>
  <span class="font-display text-[48px] text-green max-sm:hidden" aria-hidden="true">★</span>
</section>
<div class="grid grid-cols-[1fr_55px_1fr] items-center gap-2.5 max-sm:grid-cols-1 max-sm:gap-[25px]">
  <div class={column}>
    <span class="mini-label">01 / SEMIFINALS</span>
    {#each semis as g (g.id)}
      <GameCard game={g} full />
    {:else}
      <div class={placeholder}>Semifinal 1<br />Matchup to be scheduled</div>
      <div class={placeholder}>Semifinal 2<br />Matchup to be scheduled</div>
    {/each}
  </div>
  <div class="border-t border-green-edge max-sm:hidden"></div>
  <div class={column}>
    <span class="mini-label">02 / CHAMPIONSHIP</span>
    {#each finals as g (g.id)}
      <GameCard game={g} full />
    {:else}
      <div class={placeholder}>
        The final matchup<br />Semifinal winners meet here. The commissioner will schedule the championship after both results are final.
      </div>
    {/each}
  </div>
</div>
