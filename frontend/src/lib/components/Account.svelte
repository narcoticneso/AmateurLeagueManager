
<script lang="ts">
  import { api, apiBlob, type ViewInput } from "$lib/api";
  import { Button } from "$lib/components/ui/button";
  import { titleCase } from "$lib/format";
  import {
    app,
    openModal,
    refresh,
    showToast,
    teamName,
  } from "$lib/league.svelte";
  import SelectField from "./SelectField.svelte";

  const viewOptions = ["COMMISSIONER", "MANAGER", "PLAYER"].map((v) => ({
    value: v,
    label: `${titleCase(v)} view`,
  }));

  let profileOpen = $state(false);
  let profilePhoto = $state("");
  let photoInput = $state<HTMLInputElement>();
  let photoRequest = 0;

    async function loadProfilePhoto() {
      const request = ++photoRequest;

      try {
        const image = await apiBlob("/profile-picture");

        // Ignore outdated requests, such as one finishing after sign-out.
        if (request !== photoRequest) return;

        if (profilePhoto) {
          URL.revokeObjectURL(profilePhoto);
        }

        profilePhoto = image ? URL.createObjectURL(image) : "";
      } catch (error) {
        if (request === photoRequest) {
          showToast((error as Error).message);
        }
      }
    }
	
	$effect(() => {
      const username = app.session.username;

      if (username) {
        void loadProfilePhoto();
      } else {
        photoRequest++;

        if (profilePhoto) {
          URL.revokeObjectURL(profilePhoto);
        }

        profilePhoto = "";
      }
    });

  async function changeView(view: string) {
    try {
      const input: ViewInput = { view };
      await api("/view", "POST", input);
      await refresh();
      showToast(`${titleCase(app.session.view)} view enabled.`);
    } catch (error) {
      showToast((error as Error).message);
    }
  }

  async function signOut() {
    try {
      await api("/logout", "POST");
      profileOpen = false;
      await refresh();
      showToast("Signed out. You are viewing the public league.");
    } catch (error) {
      showToast((error as Error).message);
    }
  }

  async function handlePhotoChange(event: Event) {
      const input = event.currentTarget as HTMLInputElement;
      const file = input.files?.[0];

      if (!file) return;

      const allowedTypes = [
        "image/jpeg",
        "image/png",
        "image/gif",
        "image/webp",
      ];

      if (!allowedTypes.includes(file.type)) {
        showToast("Please upload a JPEG, PNG, GIF, or WebP image.");
        input.value = "";
        return;
      }

      if (file.size > 5 * 1024 * 1024) {
        showToast("Profile pictures must be smaller than 5 MB.");
        input.value = "";
        return;
      }

      try {
        const formData = new FormData();
        formData.append("file", file);

        await api("/profile-picture", "POST", formData);

        await loadProfilePhoto();

        showToast("Profile picture saved!");
      } catch (error) {
        showToast((error as Error).message);
      } finally {
        input.value = "";
      }
    }
</script>

<div class="flex items-center gap-2.5">
  {#if app.session.username}
    <SelectField
      bind:value={() => app.session.view, changeView}
      options={viewOptions}
      label="View as"
      class="w-auto bg-raised py-[9px] pr-2.5 pl-2.5 text-[11px] max-sm:text-[10px]"
    />

    <div
      class="relative"
      role="presentation"
      onmouseenter={() => (profileOpen = true)}
      onmouseleave={() => (profileOpen = false)}
    >
      <button
        type="button"
        onclick={() => (profileOpen = !profileOpen)}
        aria-label="Open profile"
        aria-expanded={profileOpen}
        class="grid size-[30px] place-items-center overflow-hidden rounded-full border border-edge bg-green-tint text-xs font-bold text-green"
        title={titleCase(app.session.username)}
      >
        {#if profilePhoto}
          <img
            src={profilePhoto}
            alt="Profile"
            class="size-full rounded-full object-cover"
          />
        {:else}
          {app.session.username.charAt(0).toUpperCase()}
        {/if}
      </button>

      {#if profileOpen}
        <div
          class="absolute right-0 top-full z-50 mt-2 w-64 rounded-xl border border-line bg-surface p-4 shadow-xl"
        >
          <div class="flex items-center gap-3">
            <div class="grid size-12 shrink-0 place-items-center overflow-hidden rounded-full border border-edge bg-green-tint text-lg font-bold text-green">
              {#if profilePhoto}
                <img
                  src={profilePhoto}
                  alt="Profile"
                  class="size-full rounded-full object-cover"
                />
              {:else}
                {app.session.username.charAt(0).toUpperCase()}
              {/if}
            </div>

            <div class="min-w-0">
              <strong class="block truncate text-sm">
                {titleCase(app.session.username)}
              </strong>
              <span class="text-xs text-muted">
                {teamName(app.session.teamId) || "League account"}
              </span>
            </div>
          </div>

          <div class="my-3 border-t border-line"></div>

          <div class="flex items-center justify-between text-xs">
            <span class="text-muted">Current role</span>
            <strong>{titleCase(app.session.view)}</strong>
          </div>

          <button
            type="button"
            onclick={() => photoInput?.click()}
            class="mt-4 flex w-full items-center justify-center gap-2 rounded-md border border-line px-3 py-2 text-xs font-medium transition hover:bg-panel-hover"
          >
            Edit profile picture
          </button>

          <input
            bind:this={photoInput}
            type="file"
            accept="image/*"
            class="hidden"
            onchange={handlePhotoChange}
          />
        </div>
      {/if}
    </div>

    <Button variant="plain" onclick={signOut}>Sign out</Button>
  {:else}
    <Button
      variant="secondary"
      size="small"
      onclick={() => openModal("login")}
    >
      Developer sign in ↗
    </Button>
  {/if}
</div>
