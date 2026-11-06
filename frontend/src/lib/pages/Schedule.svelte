<script lang="ts">
  import EmptyState from "$lib/components/EmptyState.svelte";
  import GameCard from "$lib/components/GameCard.svelte";
  import PageHeading from "$lib/components/PageHeading.svelte";
  import ScheduleButton from "$lib/components/ScheduleButton.svelte";
  import SelectField from "$lib/components/SelectField.svelte";
  import { Button } from "$lib/components/ui/button";
  import { app, commissioner, filters, openModal, teamName } from "$lib/league.svelte";
  import { formHelp, panel, panelHeader, upcomingList, upcomingRow } from "$lib/styles";
  import { cn } from "$lib/utils";

  const statusFilters = [
    ["ALL", "All games"],
    ["SCHEDULED", "Upcoming"],
    ["FINAL", "Results"],
    ["POSTPONED", "Postponed"],
    ["CANCELLED", "Cancelled"],
  ] as const;

  const games = $derived(app.league.games.filter((g) => (filters.schedule === "ALL" || g.status === filters.schedule) && (filters.week === "ALL" || g.week === Number(filters.week))));
  const weekOptions = $derived([{ value: "ALL", label: "All weeks" }, ...[...new Set(app.league.games.map((g) => g.week))].sort((a, b) => a - b).map((w) => ({ value: String(w), label: `Week ${w}` }))]);

  const myGames = $derived(app.league.games.filter((g) => (g.homeId === app.session.teamId || g.awayId === app.session.teamId) && g.status === "SCHEDULED"));
  const pending = $derived(
    app.league.scoreSubmissions.flatMap((s) => {
      const game = myGames.find((g) => g.id === s.gameId);
      return s.status === "PENDING" && s.submittedBy !== app.session.username && game ? [{ submission: s, game }] : [];
    }),
  );
  const disputed = $derived(
    app.league.scoreSubmissions.flatMap((s) => {
      const game = app.league.games.find((g) => g.id === s.gameId);
      return s.status === "DISPUTED" && game ? [{ submission: s, game }] : [];
    }),
  );
  const disputedCount = $derived(app.league.scoreSubmissions.filter((s) => s.status === "DISPUTED").length);
</script>

<PageHeading eyebrow="GET READY FOR GAME DAY" title="League schedule" description="Every matchup, from the opening kickoff to the final whistle.">
  {#snippet action()}
    {#if commissioner()}<ScheduleButton />{/if}
  {/snippet}
</PageHeading>

<div class="mb-[22px] flex flex-wrap items-center gap-2.5 max-sm:gap-[7px]">
  {#each statusFilters as [value, label] (value)}
    <Button variant="filter" aria-pressed={filters.schedule === value} onclick={() => (filters.schedule = value)}>{label}</Button>
  {/each}
  <SelectField bind:value={filters.week} options={weekOptions} label="Filter by week" class="w-auto min-w-[150px] max-sm:max-w-full max-sm:min-w-0 max-sm:text-[11px]" />
</div>
<div class="grid grid-cols-[repeat(3,minmax(0,1fr))] gap-[17px] max-lg:grid-cols-2 max-sm:grid-cols-1">
  {#each games as g (g.id)}
    <GameCard game={g} full />
  {/each}
</div>
{#if !games.length}
  <EmptyState title="No games here yet" description="Choose another filter or schedule a new matchup." />
{/if}

{#if app.session.username && app.session.view === "MANAGER"}
  <section class={cn(panel, "mt-7")}>
    <div class={panelHeader}>
      <h2>Game scores</h2>
      <span class="mini-label">MANAGER</span>
    </div>
    <p class={formHelp}>Submit a score after a game, or respond to an opposing manager's submission.</p>
    <div class={upcomingList}>
      {#each myGames as g (g.id)}
        <div class={upcomingRow}>
          <strong>{teamName(g.awayId)} vs {teamName(g.homeId)}</strong>
          <Button variant="secondary" size="small" onclick={() => openModal("submit-score", g.id)}>Submit score</Button>
        </div>
      {/each}
      {#each pending as { submission, game } (submission.id)}
        <div class={upcomingRow}>
          <strong
            >{teamName(game.awayId)}
            {submission.submittedAwayScore} – {submission.submittedHomeScore}
            {teamName(game.homeId)}</strong
          >
          <div>
            <Button size="small" onclick={() => openModal("confirm-score", submission.id)}>Confirm score</Button>
            <Button variant="secondary" size="small" onclick={() => openModal("dispute-score", submission.id)}>Dispute score</Button>
          </div>
        </div>
      {/each}
    </div>
  </section>
{/if}

{#if commissioner()}
  <section class={cn(panel, "mt-7")}>
    <div class={panelHeader}>
      <h2>Disputed scores</h2>
      <span class="mini-label">{disputedCount} DISPUTED</span>
    </div>
    <div class={upcomingList}>
      {#each disputed as { submission, game } (submission.id)}
        <div class={upcomingRow}>
          <strong>
            {teamName(game.awayId)}
            {submission.submittedAwayScore} – {submission.submittedHomeScore}
            {teamName(game.homeId)}
          </strong>
          <p class={formHelp}>
            Submitted by: {submission.submittedBy}
            <br />
            Opposing manager's score:
            {submission.disputedAwayScore} – {submission.disputedHomeScore}
          </p>
          <Button size="small" onclick={() => openModal("resolve-score", submission.id)}>Resolve dispute</Button>
        </div>
      {/each}
      {#if disputedCount === 0}
        <p class="text-muted">No disputed scores need attention.</p>
      {/if}
    </div>
  </section>
{/if}
