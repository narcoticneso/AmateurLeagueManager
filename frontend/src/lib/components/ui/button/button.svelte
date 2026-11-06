<script lang="ts" module>
  import { type VariantProps, tv } from "tailwind-variants";
  import { cn, type WithElementRef } from "$lib/utils.js";
  import type { HTMLAnchorAttributes, HTMLButtonAttributes } from "svelte/elements";

  export const buttonVariants = tv({
    base: "inline-flex items-center justify-center gap-2 whitespace-nowrap",
    variants: {
      variant: {
        primary: "min-h-10 rounded-md border border-transparent bg-green px-4 py-[11px] text-xs font-bold text-ink hover:bg-green-hover",
        secondary: "min-h-10 rounded-md border border-edge bg-raised px-4 py-[11px] text-xs font-bold text-soft hover:bg-raised-hover",
        text: "inline border-0 bg-transparent px-0 py-2 text-[11px] text-green hover:underline",
        danger: "inline border-0 bg-transparent px-0 py-2 text-[11px] text-danger hover:underline",
        icon: "size-8 rounded-[5px] border border-edge bg-raised text-soft",
        filter: "rounded-[5px] border border-edge bg-surface px-3.5 py-[9px] text-[11px] text-muted aria-pressed:border-green aria-pressed:bg-green aria-pressed:font-bold aria-pressed:text-ink",
        plain: "border-0 bg-transparent p-2 text-[11px] text-muted max-sm:p-1 max-sm:text-[10px]",
      },
      size: {
        default: "",
        small: "min-h-8 px-3 py-[7px] text-[11px]",
      },
    },
    defaultVariants: {
      variant: "primary",
      size: "default",
    },
  });

  export type ButtonVariant = VariantProps<typeof buttonVariants>["variant"];
  export type ButtonSize = VariantProps<typeof buttonVariants>["size"];

  export type ButtonProps = WithElementRef<HTMLButtonAttributes> &
    WithElementRef<HTMLAnchorAttributes> & {
      variant?: ButtonVariant;
      size?: ButtonSize;
    };
</script>

<script lang="ts">
  let { class: className, variant = "primary", size = "default", ref = $bindable(null), href, type = "button", disabled, children, ...restProps }: ButtonProps = $props();
</script>

{#if href}
  <a bind:this={ref} data-slot="button" class={cn(buttonVariants({ variant, size }), className)} href={disabled ? undefined : href} aria-disabled={disabled} role={disabled ? "link" : undefined} tabindex={disabled ? -1 : undefined} {...restProps}>
    {@render children?.()}
  </a>
{:else}
  <button bind:this={ref} data-slot="button" class={cn(buttonVariants({ variant, size }), className)} {type} {disabled} {...restProps}>
    {@render children?.()}
  </button>
{/if}
