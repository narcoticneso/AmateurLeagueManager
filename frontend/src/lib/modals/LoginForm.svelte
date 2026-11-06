<script lang="ts">
  import { api } from "$lib/api";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  let username = $state("");
  let password = $state("");

  async function signIn() {
    await api(
      "/login",
      "POST",
      new URLSearchParams({
        username: username.trim().toLowerCase(),
        password,
      }),
    );
    return "Welcome back. Your field is ready.";
  }
</script>

<FormDialog title="Welcome to the clubhouse" submitText="Sign in" onsubmit={signIn}>
  <p class={formHelp}>
    Developer accounts: rudra, ryan, lucas, nebojsa, and kiara. Each account can switch between commissioner, manager, and player views. Local demo password: <strong>neighborhood-demo</strong> (unless configured otherwise).
  </p>
  <div class={formGrid}>
    <Label class="col-span-full">Username<Input bind:value={username} autocomplete="username" placeholder="e.g. rudra" required /></Label>
    <Label class="col-span-full">Password<Input type="password" bind:value={password} autocomplete="current-password" required /></Label>
  </div>
</FormDialog>
