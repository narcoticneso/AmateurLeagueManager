<script lang="ts">
  import PageHeading from "$lib/components/PageHeading.svelte";
  import StandingsTable from "$lib/components/StandingsTable.svelte";
  import { app } from "$lib/league.svelte";
  import { panel, panelHeader, tableFooter } from "$lib/styles";
</script>

<PageHeading eyebrow="THE RACE TO THE TOP" title="League standings" description="Every game counts. Follow the regular-season table." />

<section class={panel}>
  <div class={panelHeader}>
    <h2>Fall 2026 standings</h2>
    <span class="mini-label">{app.league.teams.length} TEAMS</span>
  </div>
  <StandingsTable />
  <div class={tableFooter}>WIN % = (wins + ½ ties) ÷ games played. Tiebreak: point differential. Equal records and differential use a stable team-ID order, not an official playoff seed. Tournament results are excluded.</div>
</section>
