<script lang="ts">
  import type { Snippet } from "svelte";

  let { title, description, action }: { title: string; description: string; action?: Snippet } = $props();
</script>

<div class="rounded-[9px] border border-dashed border-edge px-[25px] py-[45px] text-center text-muted">
  <h2 class="mb-2.5 text-text">{title}</h2>
  <p class="text-xs leading-[1.8]">{description}</p>
  {#if action}
    <div class="mt-[17px]">{@render action()}</div>
  {/if}
</div>
