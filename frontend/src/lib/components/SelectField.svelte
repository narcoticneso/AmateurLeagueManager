<script lang="ts">
  import * as Select from "$lib/components/ui/select";

  let {
    value = $bindable(),
    options,
    label,
    class: className = "",
    onchange,
  }: {
    value: string;
    options: { value: string; label: string }[];
    label?: string;
    class?: string;
    onchange?: (value: string) => void;
  } = $props();

  const selected = $derived(options.find((o) => o.value === value));
</script>

<Select.Root type="single" items={options} bind:value onValueChange={(next: string) => onchange?.(next)}>
  <Select.Trigger aria-label={label} class={className}>
    {selected?.label ?? ""}
  </Select.Trigger>
  <Select.Content>
    {#each options as option (option.value)}
      <Select.Item value={option.value} label={option.label} />
    {/each}
  </Select.Content>
</Select.Root>
