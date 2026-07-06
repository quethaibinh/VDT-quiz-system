export function buildMonitoringWebSocketUrl(apiBaseUrl: string, token: string, origin = window.location.origin) {
  const base = apiBaseUrl.startsWith("/")
    ? new URL(apiBaseUrl, origin)
    : new URL(apiBaseUrl);
  base.protocol = base.protocol === "https:" ? "wss:" : "ws:";
  const path = joinPaths(base.pathname, "/v1/api/examruntime-service/ws");
  base.pathname = path;
  base.search = "";
  base.searchParams.set("access_token", token);
  return base.toString();
}

function joinPaths(left: string, right: string) {
  const prefix = left.endsWith("/") ? left.slice(0, -1) : left;
  const suffix = right.startsWith("/") ? right : `/${right}`;
  if (!prefix) return suffix;
  return `${prefix}${suffix}`;
}
