<script lang="ts">
  import XIcon from "@lucide/svelte/icons/x";
  import type { Snippet } from "svelte";
  import { Button } from "$lib/components/ui/button";
  import * as Dialog from "$lib/components/ui/dialog";
  import { closeModal, refresh, showToast } from "$lib/league.svelte";

  let {
    title,
    submitText,
    onsubmit,
    children,
  }: {
    title: string;
    submitText: string;
    onsubmit: () => Promise<string>;
    children: Snippet;
  } = $props();

  let error = $state("");
  let saving = $state(false);

  async function submit(event: SubmitEvent) {
    event.preventDefault();
    saving = true;
    error = "";
    try {
      const message = await onsubmit();
      closeModal();
      await refresh();
      showToast(message);
    } catch (failure) {
      error = (failure as Error).message;
    } finally {
      saving = false;
    }
  }
</script>

<Dialog.Root open onOpenChange={(open) => !open && closeModal()}>
  <Dialog.Content interactOutsideBehavior="ignore">
    <form onsubmit={submit}>
      <div class="mb-[25px] flex items-center justify-between gap-[15px]">
        <div>
          <span class="mini-label">LEAGUE MANAGER</span>
          <Dialog.Title>{title}</Dialog.Title>
        </div>
        <Button variant="icon" aria-label="Close dialog" onclick={closeModal}>
          <XIcon class="size-4" />
        </Button>
      </div>
      {@render children()}
      <p class="mt-[15px] text-xs leading-[1.5] text-error" role="alert">
        {error}
      </p>
      <div class="mt-[22px] flex justify-end gap-[9px]">
        <Button variant="secondary" onclick={closeModal}>Cancel</Button>
        <Button type="submit" disabled={saving}>{submitText}</Button>
      </div>
    </form>
  </Dialog.Content>
</Dialog.Root>
