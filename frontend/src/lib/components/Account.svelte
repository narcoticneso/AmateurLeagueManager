<script lang="ts">
  import { api, type ViewInput } from "$lib/api";
  import { Button } from "$lib/components/ui/button";
  import { titleCase } from "$lib/format";
  import { app, openModal, refresh, showToast } from "$lib/league.svelte";
  import SelectField from "./SelectField.svelte";

  const viewOptions = $derived(
    ["PLAYER", ...(app.session.accountRole === "COMMISSIONER" || app.session.memberships.some((membership) => membership.role === "MANAGER") ? ["MANAGER"] : []), ...(app.session.accountRole === "COMMISSIONER" ? ["COMMISSIONER"] : [])].map((v) => ({
      value: v,
      label: `${titleCase(v)} view`,
    })),
  );

  async function changeView(view: string) {
    try {
      const input: ViewInput = { view };
      await api("/view", "POST", input);
      await refresh();
      showToast(`${titleCase(app.session.view)} view enabled.`);
    } catch (error) {
      showToast((error as Error).message);
    }
  }

  async function signOut() {
    try {
      await api("/logout", "POST");
      await refresh();
      showToast("Signed out. You are viewing the public league.");
    } catch (error) {
      showToast((error as Error).message);
    }
  }
</script>

<div class="flex items-center gap-2.5">
  {#if app.session.username}
    <SelectField bind:value={() => app.session.view, changeView} options={viewOptions} label="View as" class="w-auto bg-raised py-[9px] pr-2.5 pl-2.5 text-[11px] max-sm:text-[10px]" />
    <span class="grid size-[30px] place-items-center rounded-full border border-edge bg-green-tint text-xs font-bold text-green max-sm:hidden" title={titleCase(app.session.username)}>{app.session.username.charAt(0).toUpperCase()}</span>
    <Button variant="plain" onclick={signOut}>Sign out</Button>
  {:else}
    <Button variant="secondary" size="small" onclick={() => openModal("login")}>Sign in ↗</Button>
  {/if}
</div>
