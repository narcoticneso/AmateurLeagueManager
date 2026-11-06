<script lang="ts">
  import EmptyState from "$lib/components/EmptyState.svelte";
  import GameCard from "$lib/components/GameCard.svelte";
  import PageHeading from "$lib/components/PageHeading.svelte";
  import ScheduleButton from "$lib/components/ScheduleButton.svelte";
  import StandingsTable from "$lib/components/StandingsTable.svelte";
  import TeamBadge from "$lib/components/TeamBadge.svelte";
  import { date, phaseName, time } from "$lib/format";
  import { app, record, team } from "$lib/league.svelte";
  import { panel, panelHeader, tableFooter, upcomingList, upcomingRow } from "$lib/styles";
  import { cn } from "$lib/utils";

  const finals = $derived(app.league.games.filter((g) => g.status === "FINAL"));
  const recent = $derived(finals.slice(-3).reverse());
  const upcoming = $derived(app.league.games.filter((g) => g.status === "SCHEDULED"));
  const next = $derived(upcoming[0]);
  const twoColumns = "grid grid-cols-[minmax(0,1.8fr)_minmax(245px,1fr)] gap-5 max-lg:grid-cols-[minmax(0,1.5fr)_minmax(235px,1fr)] max-lg:gap-[15px] max-md:grid-cols-1 xl:grid-cols-[minmax(0,1.85fr)_minmax(300px,1fr)]";
  const heroTeam = "[&_h3]:mt-[9px] [&_h3]:text-[23px] max-lg:[&_h3]:text-[20px] max-sm:[&_h3]:mx-auto max-sm:[&_h3]:mt-2.5 max-sm:[&_h3]:max-w-[110px] max-sm:[&_h3]:text-[21px] xl:[&_h3]:text-[27px]";
  const heroRecord = "mt-1 text-[10px] text-muted max-sm:text-[9px]";
  const seasonStat = "[&_span]:text-[9px] [&_span]:text-muted [&_strong]:block [&_strong]:font-display [&_strong]:text-[27px] [&_strong]:font-semibold";
</script>

<PageHeading eyebrow="FALL 2026 / LEAGUE HUB" title="Your league. All here." description="The matchups, the moments, and the race to the top.">
  {#snippet action()}<ScheduleButton />{/snippet}
</PageHeading>

<div class="mb-3 flex items-center justify-between">
  <h2 class="font-sans text-xs font-semibold">Last time on the field</h2>
  <span class="flex items-center gap-[7px] text-[10px] text-muted max-sm:text-[8px]"><span class="green-dot"></span> Sample season · {app.league.teams.length} neighborhood teams</span>
</div>
<div class="mb-[27px] grid grid-cols-3 gap-3 max-md:gap-2 max-sm:grid-cols-[repeat(3,minmax(0,1fr))] max-sm:gap-[7px]">
  {#each recent as g (g.id)}
    <article class="rounded-lg border border-line bg-panel px-[17px] py-3.5 max-lg:p-3 max-sm:px-2 max-sm:py-2.5">
      <div class="mb-3 flex justify-between text-[9px] tracking-[0.7px] text-muted max-sm:text-[7px] max-sm:tracking-normal">
        <span>WEEK {g.week} · {phaseName(g.phase).toUpperCase()}</span><span>FINAL</span>
      </div>
      <div class="flex items-center justify-between gap-1.5 max-sm:flex-wrap max-sm:gap-[7px]">
        <span class="flex items-center gap-2 text-[11px] font-bold max-lg:gap-[5px] max-sm:text-[9px]"><TeamBadge id={g.awayId} size="small" class="max-md:hidden" />{team(g.awayId)?.abbreviation}</span>
        <strong class="font-display text-[25px] leading-normal font-semibold tracking-[1px] whitespace-nowrap max-md:text-[22px] max-sm:order-3 max-sm:w-full max-sm:text-center max-sm:text-[21px]"
          >{g.awayScore}<span class="px-[3px] text-faint">:</span>{g.homeScore}</strong
        >
        <span class="flex items-center gap-2 text-[11px] font-bold max-lg:gap-[5px] max-sm:text-[9px]">{team(g.homeId)?.abbreviation}<TeamBadge id={g.homeId} size="small" class="max-md:hidden" /></span>
      </div>
    </article>
  {/each}
</div>

<div class={cn(twoColumns, "mb-[26px] max-sm:mb-5")}>
  {#if next}
    <section
      class="relative min-h-[272px] overflow-hidden rounded-[10px] border border-edge bg-linear-[110deg] from-hero-from via-hero-via via-55% to-hero-to px-[27px] pt-[23px] before:pointer-events-none before:absolute before:inset-0 before:bg-[repeating-linear-gradient(90deg,transparent_0,transparent_calc(20%-1px),var(--color-hero-stripe)_20%)] before:opacity-[0.14] before:content-[''] max-lg:px-[17px] max-lg:pt-5 max-sm:min-h-[260px] max-sm:px-[15px] max-sm:pt-[19px] xl:min-h-[300px]"
    >
      <div class="relative flex items-center justify-between">
        <span class="flex items-center gap-[7px] text-[9px] font-bold tracking-[1.3px] text-green max-sm:text-[8px]"><span class="green-dot"></span> NEXT ON THE FIELD</span>
        <span class="text-[9px] tracking-[1px] text-muted max-sm:text-[8px]">WEEK {next.week} · {phaseName(next.phase).toUpperCase()}</span>
      </div>
      <div class="relative mt-[25px] mb-6 grid grid-cols-[1fr_100px_1fr] items-center text-center max-lg:grid-cols-[1fr_76px_1fr] max-sm:my-6 max-sm:grid-cols-[1fr_75px_1fr] xl:my-[31px]">
        <div class={heroTeam}>
          <TeamBadge id={next.awayId} size="large" />
          <h3>{team(next.awayId)?.name}</h3>
          <div class={heroRecord}>{record(next.awayId)} · AWAY</div>
        </div>
        <div class="font-display text-[21px] font-semibold text-muted">
          VS<strong class="mt-1.5 block text-[27px] tracking-[0.3px] whitespace-nowrap text-text max-sm:text-[24px]">{time(next.kickoff)}</strong><small class="mt-1.5 block font-sans text-[9px] font-normal tracking-[0.5px]"
            >{date(next.kickoff, { weekday: "short" }).toUpperCase()}</small
          >
        </div>
        <div class={heroTeam}>
          <TeamBadge id={next.homeId} size="large" />
          <h3>{team(next.homeId)?.name}</h3>
          <div class={heroRecord}>{record(next.homeId)} · HOME</div>
        </div>
      </div>
      <div class="relative flex items-center justify-between gap-3 border-t border-edge py-3.5 text-[10px] text-soft max-sm:gap-2 max-sm:py-[15px] max-sm:text-[8px]">
        <span>⌖ &nbsp;{next.location}</span>
        <a class="p-0 text-[11px] whitespace-nowrap text-green hover:underline max-sm:text-[9px]" href="#schedule">Full schedule ↗</a>
      </div>
    </section>
  {:else}
    <EmptyState title="The field is clear" description="Schedule the next matchup when your teams are ready." />
  {/if}
  <section class="rounded-[10px] border border-line bg-panel p-[22px] max-lg:px-[17px] max-lg:py-5 max-md:hidden">
    <span class="flex items-center justify-between mini-label">SEASON AT A GLANCE <span class="green-dot"></span></span>
    <h3 class="mt-3.5 mb-[7px] text-[27px]">
      One neighborhood.<br />Plenty to play for.
    </h3>
    <p class="text-[11px] leading-[1.7] text-muted">
      The regular season sets the stage.<br />The Oakwood Cup settles the score.
    </p>
    <div class="mt-[19px] grid grid-cols-3 border-t border-line pt-[15px]">
      <div class={seasonStat}>
        <strong>{app.league.teams.length}</strong><span>TEAMS</span>
      </div>
      <div class={seasonStat}>
        <strong>{finals.filter((g) => g.phase === "REGULAR").length}</strong><span>GAMES PLAYED</span>
      </div>
      <div class={seasonStat}><strong>01</strong><span>TROPHY</span></div>
    </div>
    <a class="mt-3 py-2 text-[11px] text-green hover:underline" href="#playoffs">The road to the cup ↗</a>
  </section>
</div>

<div class={twoColumns}>
  <section class={panel}>
    <div class={panelHeader}>
      <h2>League standings</h2>
      <a class="py-2 text-[11px] text-green hover:underline" href="#standings">Full standings ↗</a>
    </div>
    <StandingsTable compact />
    <div class={tableFooter}>Regular season only · Sorted by win percentage, then point differential</div>
  </section>
  <section class={panel}>
    <div class={panelHeader}>
      <h2>Coming up</h2>
      <span class="mini-label">{upcoming.length} SCHEDULED</span>
    </div>
    <div class={upcomingList}>
      {#each upcoming.slice(0, 3) as g (g.id)}
        <GameCard game={g} />
      {:else}
        <div class={cn(upcomingRow, "text-muted")}>No games scheduled yet.</div>
      {/each}
    </div>
  </section>
</div>
