<script lang="ts">
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";

  let {
    awayScore = $bindable(),
    homeScore = $bindable(),
    disabled = false,
  }: {
    awayScore: number | null;
    homeScore: number | null;
    disabled?: boolean;
  } = $props();
</script>

<Label>Away score<Input type="number" min="0" max="200" required={!disabled} {disabled} bind:value={awayScore} /></Label>
<Label>Home score<Input type="number" min="0" max="200" required={!disabled} {disabled} bind:value={homeScore} /></Label>
