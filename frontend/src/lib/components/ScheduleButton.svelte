<script lang="ts">
  import { Button } from "$lib/components/ui/button";
  import { commissioner, openModal } from "$lib/league.svelte";
</script>

{#if commissioner()}
  <Button onclick={() => openModal("new-game")}>＋ Schedule game</Button>
{:else}
  <Button variant="secondary" href="#schedule">View schedule ↗</Button>
{/if}
