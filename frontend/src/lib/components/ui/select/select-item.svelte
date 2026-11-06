<script lang="ts">
  import { Select as SelectPrimitive } from "bits-ui";
  import CheckIcon from "@lucide/svelte/icons/check";
  import { cn, type WithoutChild } from "$lib/utils.js";

  let { ref = $bindable(null), class: className, value, label, children: childrenProp, ...restProps }: WithoutChild<SelectPrimitive.ItemProps> = $props();
</script>

<SelectPrimitive.Item
  bind:ref
  {value}
  {label}
  data-slot="select-item"
  class={cn(
    "relative flex w-full cursor-default items-center gap-2 rounded-[4px] py-2 pr-8 pl-2.5 text-xs outline-hidden select-none data-disabled:pointer-events-none data-disabled:opacity-50 data-highlighted:bg-accent data-highlighted:text-accent-foreground [&_svg]:pointer-events-none [&_svg]:shrink-0",
    className,
  )}
  {...restProps}
>
  {#snippet children({ selected, highlighted })}
    <span class="pointer-events-none absolute right-2 flex size-4 items-center justify-center">
      {#if selected}
        <CheckIcon class="pointer-events-none size-3.5 text-green" />
      {/if}
    </span>
    <span class="flex flex-1 shrink-0 gap-2 whitespace-nowrap">
      {#if childrenProp}
        {@render childrenProp({ selected, highlighted })}
      {:else}
        {label || value}
      {/if}
    </span>
  {/snippet}
</SelectPrimitive.Item>
