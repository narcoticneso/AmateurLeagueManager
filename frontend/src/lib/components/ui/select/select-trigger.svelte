<script lang="ts">
  import { Select as SelectPrimitive } from "bits-ui";
  import ChevronDownIcon from "@lucide/svelte/icons/chevron-down";
  import { cn, type WithoutChild } from "$lib/utils.js";

  let {
    ref = $bindable(null),
    class: className,
    children,
    size = "default",
    ...restProps
  }: WithoutChild<SelectPrimitive.TriggerProps> & {
    size?: "sm" | "default";
  } = $props();
</script>

<SelectPrimitive.Trigger
  bind:ref
  data-slot="select-trigger"
  data-size={size}
  class={cn("flex w-full min-w-0 items-center justify-between gap-2 rounded-[5px] border border-edge bg-field p-[11px] text-left text-xs whitespace-nowrap text-text disabled:opacity-55 *:data-[slot=select-value]:line-clamp-1", className)}
  {...restProps}
>
  {@render children?.()}
  <ChevronDownIcon class="pointer-events-none size-3.5 shrink-0 text-muted" />
</SelectPrimitive.Trigger>
