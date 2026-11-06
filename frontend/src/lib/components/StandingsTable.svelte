<script lang="ts">
  import { app, playerCount, record, team } from "$lib/league.svelte";
  import { numeric, table, teamCell } from "$lib/styles";
  import { cn } from "$lib/utils";
  import TeamBadge from "./TeamBadge.svelte";

  let { compact = false }: { compact?: boolean } = $props();

  const pointsCol = $derived(compact ? "max-sm:hidden" : "");
</script>

<div class="overflow-x-auto">
  <table class={table} aria-label="Regular season standings">
    <thead>
      <tr>
        <th>#</th>
        <th>TEAM</th>
        <th class={numeric}>W–L–T</th>
        <th class={numeric}>WIN %</th>
        <th class={cn(numeric, pointsCol)}>PF</th>
        {#if !compact}
          <th class={numeric}>PA</th>
          <th class={numeric}>DIFF</th>
        {/if}
      </tr>
    </thead>
    <tbody>
      {#each app.league.standings as s, i (s.teamId)}
        {@const diff = s.pointsFor - s.pointsAgainst}
        <tr>
          <td class="w-[35px] text-[11px]! text-muted">{String(i + 1).padStart(2, "0")}</td>
          <td>
            <a class={teamCell} href="#team/{s.teamId}">
              <TeamBadge id={s.teamId} />
              <span>
                <strong>{team(s.teamId)?.name}</strong>
                <small>{team(s.teamId)?.manager} · {playerCount(s.teamId)} players</small>
              </span>
            </a>
          </td>
          <td class={numeric}>{record(s.teamId)}</td>
          <td class={cn(numeric, "text-green")}>{s.percentage.toFixed(3)}</td>
          <td class={cn(numeric, pointsCol)}>{s.pointsFor}</td>
          {#if !compact}
            <td class={numeric}>{s.pointsAgainst}</td>
            <td class={numeric}>{diff > 0 ? "+" : ""}{diff}</td>
          {/if}
        </tr>
      {/each}
    </tbody>
  </table>
</div>
