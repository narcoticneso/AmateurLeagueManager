import { svelte } from "@sveltejs/vite-plugin-svelte";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [tailwindcss(), svelte()],
  resolve: {
    alias: { $lib: fileURLToPath(new URL("./src/lib", import.meta.url)) },
  },
  build: { outDir: "../target/classes/static", emptyOutDir: true },
  server: {
    proxy: { "/api": `http://127.0.0.1:${process.env.PORT ?? 8080}` },
  },
});
