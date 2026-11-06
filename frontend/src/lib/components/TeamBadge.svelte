<script lang="ts">
  import { team } from "$lib/league.svelte";
  import { cn } from "$lib/utils";

  let {
    id,
    size = "default",
    class: className = "",
  }: {
    id: string;
    size?: "small" | "default" | "large";
    class?: string;
  } = $props();

  const t = $derived(team(id));
  const color = $derived(t && /^#[0-9a-f]{6}$/i.test(t.color) ? t.color : "var(--color-green)");
</script>

{#if t}
  <span
    class={cn(
      "inline-grid shrink-0 place-items-center bg-(--team-color) font-display font-bold tracking-[0.6px] text-ink inset-ring-4 inset-ring-white/10 [clip-path:polygon(0_0,100%_0,100%_72%,50%_100%,0_72%)]",
      size === "small" && "h-[27px] w-6 pb-1 text-[10px]",
      size === "default" && "h-9 w-[33px] pb-1 text-xs",
      size === "large" && "h-[76px] w-[68px] pb-2 text-[27px] max-sm:h-[61px] max-sm:w-[53px] max-sm:text-[23px]",
      className,
    )}
    style:--team-color={color}
    aria-hidden="true">{t.abbreviation}</span
  >
{/if}
