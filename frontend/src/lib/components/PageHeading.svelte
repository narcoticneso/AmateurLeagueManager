<script lang="ts">
  import type { Snippet } from "svelte";

  let {
    eyebrow,
    title,
    description,
    action,
  }: {
    eyebrow: string;
    title: string;
    description: string;
    action?: Snippet;
  } = $props();
</script>

<div class="mb-[27px] flex items-center justify-between gap-4 max-sm:mb-6 max-sm:items-start max-sm:gap-2.5">
  <div>
    <span class="mb-[7px] block mini-label text-green max-sm:text-[8px]">{eyebrow}</span>
    <h1 class="max-md:text-[34px] max-sm:text-[31px]">{title}</h1>
    <p class="mt-[7px] text-xs text-muted max-md:max-w-[290px] max-md:leading-[1.6] max-sm:max-w-60 max-sm:text-[11px]">
      {description}
    </p>
  </div>
  <div class="flex gap-2.5 max-md:*:px-3 max-md:*:py-2.5 max-md:*:text-[11px] max-sm:pt-[15px] max-sm:*:min-h-[34px] max-sm:*:p-[9px] max-sm:*:text-[10px]">
    {@render action?.()}
  </div>
</div>
