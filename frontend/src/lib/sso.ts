export type SsoMode = "login" | "register";

const resolveDefaultSsoBase = () => {
  if (typeof window === "undefined") return "";
  // Prefer same-origin so the system Nginx can proxy `/sso/login` and `/register` to userservice.
  return window.location.origin;
};

const resolveSsoBase = () => {
  return (import.meta.env.VITE_SSO_BASE_URL as string | undefined) || resolveDefaultSsoBase();
};

const resolveCallbackUrl = (nextPath?: string) => {
  if (typeof window === "undefined") return "";
  const origin = window.location.origin;
  const callback = new URL("/sso/callback", origin);
  if (nextPath) callback.searchParams.set("next", nextPath);
  return callback.toString();
};

export const buildSsoUrl = (mode: SsoMode, nextPath?: string) => {
  const base = resolveSsoBase();
  const callback = resolveCallbackUrl(nextPath);
  const path = mode === "login" ? "/sso/login" : "/register";
  const url = new URL(path, base);
  url.searchParams.set("redirect", callback);
  return url.toString();
};

