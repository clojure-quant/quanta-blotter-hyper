/**
 * Watches the SSE heartbeat timestamp and reconnects when the stream goes stale.
 * Datastar does not auto-reconnect after the server drops the EventSource, so we
 * poll for the server to return and reload the page (restores tab render-fn + SSE).
 */
;(function () {
  const ROOT_ID = "sse-connection-status"
  const STALE_MS = 2000
  const POLL_MS = 500
  const RETRY_MS = 3000

  function tick() {
    const root = document.getElementById(ROOT_ID)
    if (!root) return

    const ts = Number(
      document.getElementById("sse-server-ts")?.getAttribute("data-server-ts") || 0,
    )
    const stale = ts > 0 && Date.now() - ts > STALE_MS
    root.classList.toggle("is-stale", stale)

    if (!stale) {
      delete root.dataset.reconnecting
      return
    }

    void tryReconnect(root)
  }

  async function tryReconnect(root) {
    if (root.dataset.reconnecting === "true") return

    const tabId = root.getAttribute("data-tab-id")
    if (!tabId) return

    const now = Date.now()
    if (now - Number(root.dataset.lastReconnect || 0) < RETRY_MS) return
    root.dataset.lastReconnect = String(now)
    root.dataset.reconnecting = "true"

    try {
      const path = window.location.pathname + window.location.search
      const ping = await fetch(path, { cache: "no-store", credentials: "same-origin" })
      if (!ping.ok) return

      window.location.reload()
    } catch {
      /* server still down */
    } finally {
      delete root.dataset.reconnecting
    }
  }

  setInterval(tick, POLL_MS)
  tick()
})()
