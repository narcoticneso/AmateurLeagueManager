<script lang="ts">
  import CalendarDays from "@lucide/svelte/icons/calendar-days";
  import ChartColumn from "@lucide/svelte/icons/chart-column";
  import LayoutGrid from "@lucide/svelte/icons/layout-grid";
  import Shirt from "@lucide/svelte/icons/shirt";
  import Trophy from "@lucide/svelte/icons/trophy";
  import Users from "@lucide/svelte/icons/users";
  import type { Component } from "svelte";
  import { route } from "$lib/router.svelte";
  import { cn } from "$lib/utils";

  let { links }: { links: [string, string][] } = $props();

  const icons: Record<string, Component<{ class?: string; strokeWidth?: number }>> = {
    overview: LayoutGrid,
    schedule: CalendarDays,
    teams: Users,
    standings: ChartColumn,
    playoffs: Trophy,
    myteam: Shirt,
  };
</script>

<aside
  class="fixed inset-y-0 left-0 z-5 flex w-[242px] flex-col border-r border-line bg-sidebar px-[22px] pt-8 pb-[22px] max-lg:w-[205px] max-lg:px-[15px] max-lg:py-[25px] max-md:w-[180px] max-md:px-3 max-md:py-[23px] max-sm:static max-sm:w-auto max-sm:border-r-0 max-sm:border-b max-sm:px-[18px] max-sm:pt-[18px] max-sm:pb-0"
>
  <a class="flex items-center gap-[11px] font-display text-[23px] font-bold tracking-[1.1px] max-lg:gap-2 max-lg:text-[20px] max-md:gap-[7px] max-md:text-[18px] max-sm:text-[22px]" href="#overview"
    ><img src="/favicon.svg" alt="" width="38" height="38" class="max-md:size-[30px] max-sm:size-[35px]" /><span
      >AMATEUR<span class="mt-0.5 block font-sans text-[8px] tracking-[2.1px] text-soft max-lg:text-[7px] max-md:text-[6px] max-sm:text-[7px]">LEAGUE MANAGER</span></span
    ></a
  >
  <div class="mt-[35px] mb-[31px] grid gap-2 rounded-[9px] border border-line bg-surface px-3.5 py-[18px] max-sm:hidden">
    <span class="mini-label">YOUR LEAGUE</span><strong class="text-[14px]">Oakwood Football</strong><span class="flex items-center justify-between text-[11px] text-muted">Neighborhood league <span class="green-dot"></span></span>
  </div>
  <span class="mx-3.5 mb-3 mini-label max-sm:hidden">WORKSPACE</span>
  <nav aria-label="Main navigation" class="max-sm:mt-[17px] max-sm:flex max-sm:gap-2 max-sm:overflow-x-auto">
    {#each links as [key, label] (key)}
      {@const Icon = icons[key]}
      {@const active = route.page === key || (route.page === "team" && key === "teams")}
      <a
        class={cn(
          "mb-[7px] flex h-[46px] items-center gap-[13px] rounded-md px-3.5 text-[13px] font-medium text-muted transition-[background] duration-150 hover:bg-panel-hover hover:text-text max-md:gap-2.5 max-md:px-2.5 max-md:text-[12px] max-sm:m-0 max-sm:h-[42px] max-sm:shrink-0 max-sm:gap-1.5 max-sm:rounded-none max-sm:border-b-2 max-sm:border-transparent max-sm:px-[9px] max-sm:text-[11px]",
          active && "bg-green-tint text-green after:ml-auto after:size-[5px] after:rounded-full after:bg-green after:content-[''] max-sm:border-green max-sm:bg-transparent max-sm:after:hidden",
        )}
        href="#{key}"
        aria-current={route.page === key ? "page" : undefined}><Icon class="size-[18px] max-sm:size-[15px]" strokeWidth={1.6} />{label}</a
      >
    {/each}
  </nav>
  <div class="mx-2.5 mt-11 mb-[25px] border-t border-line py-[22px] max-sm:hidden">
    <span class="mini-label">THE SEASON IS ON</span><strong class="mt-[13px] mb-[18px] block font-display text-[26px] leading-[1.1] font-semibold">Small league.<br />Big game energy.</strong>
    <div class="mb-[11px] h-[3px] rounded-[2px] bg-green-tint">
      <span class="block h-full w-[65%] bg-green"></span>
    </div>
    <span class="text-[10px] text-muted">Fall 2026 · Regular season</span>
  </div>
  <div class="mt-auto text-[10px] leading-[2] text-soft max-sm:hidden">
    <span class="mr-[5px] green-dot"></span> Built for your home field.<small class="block text-[9px] text-dim">Amateur League Manager · v0.1</small>
  </div>
</aside>
