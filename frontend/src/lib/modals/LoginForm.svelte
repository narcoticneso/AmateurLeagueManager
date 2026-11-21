<script lang="ts">
  import { api } from "$lib/api";
  import { Input } from "$lib/components/ui/input";
  import { Label } from "$lib/components/ui/label";
  import { formGrid, formHelp } from "$lib/styles";
  import FormDialog from "./FormDialog.svelte";

  let mode = $state("login");
  let username = $state("");
  let password = $state("");
  let displayName = $state("");
  let email = $state("");
  let code = $state("");
  let confirmPassword = $state("");

  let title = $derived(mode === "login" ? "Welcome to the clubhouse" : mode === "register" ? "Create your account" : "Verify your email");

  let submitText = $derived(mode === "login" ? "Sign in" : mode === "register" ? "Send verification code" : "Verify and sign in");

  async function submit() {
    if (mode === "login") {
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

    if (mode === "register") {
      if (password.length < 8 || password.length > 72) {
        throw new Error("Password must contain between 8 and 72 characters.");
      }

      if (password !== confirmPassword) {
        throw new Error("Passwords do not match.");
      }

      await api("/register", "POST", {
        email: email.trim(),
        displayName: displayName.trim(),
        password,
      });

      mode = "verify";
      return null;
    }

    await api("/verify", "POST", {
      email: email.trim(),
      code: code.trim(),
    });

    return "Your account is verified. Welcome to the league!";
  }
</script>

<FormDialog {title} {submitText} onsubmit={submit}>
  {#if mode === "login"}
    <p class={formHelp}>Sign in with your account email, or use a local developer username for testing.</p>

    <div class={formGrid}>
      <Label class="col-span-full">
        Username or email
        <Input bind:value={username} autocomplete="username" placeholder="Email or developer username" required />
      </Label>

      <Label class="col-span-full">
        Password
        <Input type="password" bind:value={password} autocomplete="current-password" required />
      </Label>
    </div>

    <button type="button" class="mt-4 text-sm text-green underline" onclick={() => (mode = "register")}> Create a new account </button>
  {:else if mode === "register"}
    <p class={formHelp}>Enter your name and email address. We'll send you a six-digit verification code.</p>

    <div class={formGrid}>
      <Label class="col-span-full">
        Display name
        <Input bind:value={displayName} maxlength={60} autocomplete="name" required />
      </Label>

      <Label class="col-span-full">
        Email
        <Input type="email" bind:value={email} maxlength={254} autocomplete="email" required />
      </Label>
      <Label class="col-span-full">
        Password
        <Input type="password" bind:value={password} minlength={8} maxlength={72} autocomplete="new-password" required />
      </Label>

      <Label class="col-span-full">
        Confirm password
        <Input type="password" bind:value={confirmPassword} minlength={8} maxlength={72} autocomplete="new-password" required />
      </Label>
    </div>

    <button type="button" class="mt-4 text-sm text-green underline" onclick={() => (mode = "login")}> Back to sign in </button>
  {:else}
    <p class={formHelp}>
      Enter the six-digit verification code for <strong>{email}</strong>.
    </p>

    <div class={formGrid}>
      <Label class="col-span-full">
        Verification code
        <Input bind:value={code} maxlength={6} placeholder="123456" autocomplete="one-time-code" required />
      </Label>
    </div>

    <button type="button" class="mt-4 text-sm text-green underline" onclick={() => (mode = "register")}> Back to registration </button>
  {/if}
</FormDialog>
