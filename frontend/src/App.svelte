<script lang="ts">
  import Account from "$lib/components/Account.svelte";
  import Sidebar from "$lib/components/Sidebar.svelte";
  import { Button } from "$lib/components/ui/button";
  import { app, refresh, toast } from "$lib/league.svelte";
  import Modals from "$lib/modals/Modals.svelte";
  import Overview from "$lib/pages/Overview.svelte";
  import Playoffs from "$lib/pages/Playoffs.svelte";
  import Roster from "$lib/pages/Roster.svelte";
  import Schedule from "$lib/pages/Schedule.svelte";
  import Standings from "$lib/pages/Standings.svelte";
  import Teams from "$lib/pages/Teams.svelte";
  import { route } from "$lib/router.svelte";
  import { cn } from "$lib/utils";

  const links = $derived<[string, string][]>([
    ["overview", "Overview"],
    ["schedule", "Schedule"],
    ["teams", "Teams & rosters"],
    ["standings", "Standings"],
    ["playoffs", "Playoffs"],
    ...(app.session.username ? ([["myteam", "My team"]] as [string, string][]) : []),
  ]);
  const breadcrumb = $derived(route.page === "team" ? "Team roster" : (links.find(([key]) => key === route.page)?.[1] ?? "Overview"));

  $effect(() => {
    if (app.status === "ready") {
      document.title = `${breadcrumb} · Amateur League Manager`;
    }
  });

  refresh().catch((error: unknown) => {
    app.status = "failed";
    console.error(error);
  });
</script>

<Sidebar links={app.status === "ready" ? links : []} />
<div class="ml-[242px] min-h-screen max-lg:ml-[205px] max-md:ml-[180px] max-sm:ml-0">
  <header class="flex h-20 items-center justify-between gap-4 border-b border-line px-9 max-lg:px-[25px] max-md:h-[70px] max-sm:h-[60px] max-sm:px-[18px]">
    <div class="text-[11px] whitespace-nowrap text-muted max-md:text-[10px] max-sm:hidden">
      Oakwood Football <span class="mx-3 text-faint">/</span>
      <strong class="font-medium text-soft">{app.status === "ready" ? breadcrumb : "Overview"}</strong>
    </div>
    <div class="flex items-center gap-5 max-sm:w-full max-sm:justify-between">
      <span class="rounded-[4px] border border-edge px-2.5 py-[7px] text-[9px] tracking-[1.1px] whitespace-nowrap text-soft max-lg:hidden max-sm:block max-sm:text-[8px]">FALL 2026</span>
      {#if app.status === "ready"}<Account />{:else}<div></div>{/if}
    </div>
  </header>
  <main tabindex="-1" class="mx-auto max-w-[1570px] px-9 pt-8 pb-[26px] max-lg:p-[25px] max-sm:px-[18px] max-sm:py-6 xl:pt-[42px]">
    {#if app.status === "loading"}
      <div class="p-20 text-center text-muted max-sm:px-2.5 max-sm:py-[50px]">Getting the field ready…</div>
    {:else if app.status === "failed"}
      <section class="rounded-lg border border-alert-edge bg-alert-bg p-[25px] text-alert-text">
        <h2>The clubhouse is temporarily unavailable.</h2>
        <p class="mt-2.5 mb-[15px] text-xs leading-[1.7]">Check that the Java server is running, then reload this page.</p>
        <Button variant="secondary" onclick={() => location.reload()}>Try again</Button>
      </section>
    {:else if route.page === "schedule"}
      <Schedule />
    {:else if route.page === "teams"}
      <Teams />
    {:else if route.page === "standings"}
      <Standings />
    {:else if route.page === "playoffs"}
      <Playoffs />
    {:else if route.page === "myteam"}
      <Roster id={app.session.teamId} />
    {:else if route.page === "team"}
      <Roster id={route.id} />
    {:else}
      <Overview />
    {/if}
  </main>
  <footer
    class="mx-auto flex max-w-[1570px] justify-between gap-5 px-9 pt-1 pb-[26px] text-[8px] tracking-[1px] text-dim max-lg:px-[25px] max-lg:pt-2 max-lg:pb-[25px] max-sm:px-[18px] max-sm:pt-0 max-sm:pb-[22px] max-sm:text-[7px] max-sm:leading-[1.6] max-sm:tracking-[0.6px]"
  >
    <span>LOCAL TEAMS. REAL CONNECTIONS.</span><span>Sample season · All game times Eastern</span>
  </footer>
</div>
<Modals />
<div
  role="status"
  aria-live="polite"
  class={cn(
    "pointer-events-none fixed bottom-[25px] left-[calc(50%+121px)] z-20 max-w-[calc(100vw-30px)] -translate-x-1/2 translate-y-5 rounded-[7px] bg-green px-5 py-[13px] text-xs text-ink opacity-0 shadow-[0_8px_35px_#0006] transition-all duration-200 max-lg:left-[calc(50%+102px)] max-md:left-[calc(50%+90px)] max-sm:left-1/2 max-sm:w-max",
    toast.visible && "translate-y-0 opacity-100",
  )}
>
  {toast.message}
</div>
