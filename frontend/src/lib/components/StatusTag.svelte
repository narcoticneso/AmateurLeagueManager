<script lang="ts">
  import { cn } from "$lib/utils";

  let { status }: { status: string } = $props();
</script>

<span class={cn("rounded-[3px] border border-edge px-1.5 py-[3px] text-[8px] tracking-[0.7px] whitespace-nowrap text-soft", status === "FINAL" && "bg-green/5 text-green")}>{status}</span>
