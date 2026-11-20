<script lang="ts">
  import { api } from "$lib/api";
  import { Button } from "$lib/components/ui/button";
  import { app, openModal, refresh, showToast } from "$lib/league.svelte";

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
    <span class="grid size-[30px] place-items-center rounded-full border border-edge bg-green-tint text-xs font-bold text-green max-sm:hidden" title={app.session.displayName}>{app.session.displayName.charAt(0).toUpperCase()}</span>
    <Button variant="plain" onclick={signOut}>Sign out</Button>
  {:else}
    <Button variant="secondary" size="small" onclick={() => openModal("login")}>Sign in ↗</Button>
  {/if}
</div>
