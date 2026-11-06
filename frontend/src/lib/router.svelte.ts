import { filters } from "./league.svelte";

const parse = () => (location.hash.slice(1) || "overview").split("/");

export const route = $state({ page: parse()[0], id: parse()[1] ?? "" });

window.addEventListener("hashchange", () => {
  const [page, id = ""] = parse();
  route.page = page;
  route.id = id;
  filters.position = "ALL";
  window.scrollTo(0, 0);
});
