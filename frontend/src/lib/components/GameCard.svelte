<script lang="ts">
  import type { Game } from "$lib/api";
  import { Button } from "$lib/components/ui/button";
  import { date, phaseName, time } from "$lib/format";
  import { commissioner, openModal, teamName } from "$lib/league.svelte";
  import { upcomingRow } from "$lib/styles";
  import { cn } from "$lib/utils";
  import StatusTag from "./StatusTag.svelte";
  import TeamBadge from "./TeamBadge.svelte";

  let { game, full = false }: { game: Game; full?: boolean } = $props();
</script>

<article class={full ? "rounded-lg border border-line bg-panel p-[19px]" : upcomingRow}>
  <div class={cn("mb-[13px] flex justify-between gap-2.5 text-[9px] text-muted max-md:text-[8px] max-sm:text-[10px]", full && "text-[10px] max-md:text-[10px]")}>
    <span>{date(game.kickoff, { weekday: "short" })} · {time(game.kickoff)}</span>
    <StatusTag status={game.status} />
  </div>
  {#each [game.awayId, game.homeId] as id, i (i)}
    <div class={cn("my-[9px] flex items-center gap-2.5 text-[11px] max-md:gap-1.5 max-md:text-[10px] max-sm:text-xs", full && "my-[17px] text-[13px] max-md:text-[13px] max-sm:text-[13px]")}>
      <TeamBadge {id} size="small" class="max-md:hidden max-sm:inline-grid" />
      <a href="#team/{id}"><strong class="font-medium">{teamName(id)}</strong></a>
      <span class={cn("ml-auto text-[15px] font-bold", full && "font-display text-[27px] leading-normal font-semibold")}>{game.status === "FINAL" ? (i === 0 ? game.awayScore : game.homeScore) : "—"}</span>
    </div>
  {/each}
  <div class={cn("mt-3 text-[9px] text-dim", full && "border-t border-line pt-3.5 leading-[1.7]")}>
    {#if full}WEEK {game.week} · {phaseName(game.phase)}<br />{/if}{game.location}{#if full}<br />Away team listed first · All times Eastern{/if}
  </div>
  {#if full && commissioner()}
    <Button variant="text" class="mt-[9px]" onclick={() => openModal("edit-game", game.id)}>{game.status === "FINAL" ? "Edit result" : "Manage game"} ↗</Button>
  {/if}
</article>
