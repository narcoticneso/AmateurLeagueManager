<script lang="ts">
  import { cn, type WithElementRef } from "$lib/utils.js";
  import type { HTMLInputAttributes, HTMLInputTypeAttribute } from "svelte/elements";

  type InputType = Exclude<HTMLInputTypeAttribute, "file">;

  type Props = WithElementRef<
    Omit<HTMLInputAttributes, "type" | "value"> & {
      value?: string | number | null;
    } & ({ type: "file"; files?: FileList } | { type?: InputType; files?: undefined })
  >;

  let { ref = $bindable(null), value = $bindable(), type, files = $bindable(), class: className, "data-slot": dataSlot = "input", ...restProps }: Props = $props();
</script>

{#if type === "file"}
  <input
    bind:this={ref}
    data-slot={dataSlot}
    class={cn("w-full min-w-0 rounded-[5px] border border-edge bg-field p-[11px] text-xs text-text placeholder:text-dim disabled:opacity-55 [&[type=color]]:h-10 [&[type=color]]:p-[5px]", className)}
    type="file"
    bind:files
    bind:value
    {...restProps}
  />
{:else}
  <input
    bind:this={ref}
    data-slot={dataSlot}
    class={cn("w-full min-w-0 rounded-[5px] border border-edge bg-field p-[11px] text-xs text-text placeholder:text-dim disabled:opacity-55 [&[type=color]]:h-10 [&[type=color]]:p-[5px]", className)}
    {type}
    bind:value
    {...restProps}
  />
{/if}
