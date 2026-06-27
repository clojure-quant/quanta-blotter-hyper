import { GoldenLayout } from "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/+esm"

let layoutInstance = null
let resizeHandler = null
let notificationsItem = null
const panelContainers = {}

let destroyTimer = null
let initTimer = null
let attachTimer = null
const DESTROY_DELAY_MS = 100
const INIT_DEBOUNCE_MS = 50

const PANEL_IDS = ["panel-positions", "panel-trades", "panel-notifications"]

const LAYOUT_CONFIG = {
  root: {
    type: "column",
    content: [
      {
        type: "component",
        componentType: "memo",
        title: "Memo",
        size: "15%",
        minSize: "80px",
      },
      {
        type: "row",
        content: [
          {
            type: "component",
            componentType: "positions",
            title: "Positions",
            size: "50%",
            minSize: "120px",
          },
          {
            type: "component",
            componentType: "trades",
            title: "Trades",
            size: "50%",
            minSize: "120px",
          },
        ],
      },
    ],
  },
}

function fillContainer(container, child) {
  const parent = container.element
  parent.style.position = "relative"
  parent.style.height = "100%"
  parent.style.width = "100%"
  parent.style.margin = "0"
  parent.style.padding = "0"
  parent.style.overflow = "hidden"
  parent.style.minHeight = "0"
  parent.appendChild(child)
}

function getPanelStash() {
  return document.getElementById("panel-stash")
}

/** Move stream panels back into #panel-stash so Hyper can morph the layout page. */
function restoreStreamPanels() {
  const stash = getPanelStash()
  if (!stash) {
    return
  }
  for (const id of PANEL_IDS) {
    const el = document.getElementById(id)
    if (el && el.parentElement !== stash) {
      stash.appendChild(el)
    }
  }
}

function attachStreamPanel(panelElId, containerKey) {
  const host = document.getElementById("golden-layout-host")
  if (!host || host.dataset.glInit !== "true") {
    return
  }
  const el = document.getElementById(panelElId)
  const container = panelContainers[containerKey]
  if (!el || !container) {
    return
  }
  if (!container.contains(el)) {
    container.innerHTML = ""
    fillContainer({ element: container }, el)
  }
}

function attachStreamPanels() {
  attachStreamPanel("panel-positions", "positions")
  attachStreamPanel("panel-trades", "trades")
  attachStreamPanel("panel-notifications", "notifications")
}

function registerComponents(layout) {
  layout.registerComponentFactoryFunction("memo", (container) => {
    const el = document.createElement("div")
    el.className = "memo-panel"
    el.innerHTML = "<p>welcome to ant-man-trading</p>"
    fillContainer(container, el)
  })

  layout.registerComponentFactoryFunction("positions", (container) => {
    panelContainers.positions = container.element
    attachStreamPanel("panel-positions", "positions")
  })

  layout.registerComponentFactoryFunction("trades", (container) => {
    panelContainers.trades = container.element
    attachStreamPanel("panel-trades", "trades")
  })

  layout.registerComponentFactoryFunction("notifications", (container) => {
    panelContainers.notifications = container.element
    attachStreamPanel("panel-notifications", "notifications")
  })
}

function ensureMount(host) {
  let mount = host.querySelector("#gl-mount")
  if (!mount) {
    mount = document.createElement("div")
    mount.id = "gl-mount"
    mount.style.height = "100%"
    mount.style.width = "100%"
    host.appendChild(mount)
  }
  return mount
}

function cancelScheduledDestroy() {
  if (destroyTimer) {
    window.clearTimeout(destroyTimer)
    destroyTimer = null
  }
}

function scheduleDestroy() {
  if (destroyTimer) {
    window.clearTimeout(destroyTimer)
  }
  destroyTimer = window.setTimeout(() => {
    destroyTimer = null
    if (!document.getElementById("golden-layout-host")) {
      destroyLayout()
    }
  }, DESTROY_DELAY_MS)
}

function updateNotificationsToggleButton() {
  const btn = document.getElementById("toggle-notifications-btn")
  if (!btn) {
    return
  }
  btn.textContent = notificationsItem ? "Hide notifications" : "Show notifications"
}

function bindLayoutToolbar() {
  const btn = document.getElementById("toggle-notifications-btn")
  if (!btn || btn.dataset.antmanBound === "true") {
    return
  }
  btn.dataset.antmanBound = "true"
  btn.addEventListener("click", (event) => {
    event.preventDefault()
    toggleNotificationsPanel()
  })
}

function showNotificationsPanel() {
  if (!layoutInstance || notificationsItem) {
    return false
  }
  restoreStreamPanels()
  const root = layoutInstance.rootItem
  let item = null
  try {
    if (root?.isColumn) {
      item = root.newItem(
        {
          type: "component",
          componentType: "notifications",
          title: "Notifications",
          size: "22%",
          minSize: "100px",
        },
        root.contentItems.length,
      )
    } else {
      item = layoutInstance.newComponent(
        "notifications",
        undefined,
        "Notifications",
      )
    }
  } catch (err) {
    console.error("antman: show notifications failed", err)
    return false
  }
  if (!item) {
    try {
      item = layoutInstance.newComponent(
        "notifications",
        undefined,
        "Notifications",
      )
    } catch (err) {
      console.error("antman: newComponent fallback failed", err)
      return false
    }
  }
  if (!item) {
    return false
  }
  notificationsItem = item
  attachStreamPanel("panel-notifications", "notifications")
  updateNotificationsToggleButton()
  const host = document.getElementById("golden-layout-host")
  if (host) {
    resizeLayout(host)
  }
  return true
}

function hideNotificationsPanel() {
  if (!notificationsItem) {
    return false
  }
  restoreStreamPanels()
  try {
    notificationsItem.close()
  } catch (_) {
    /* already closed */
  }
  notificationsItem = null
  panelContainers.notifications = null
  updateNotificationsToggleButton()
  const host = document.getElementById("golden-layout-host")
  if (host) {
    resizeLayout(host)
  }
  return true
}

function toggleNotificationsPanel() {
  if (notificationsItem) {
    return hideNotificationsPanel()
  }
  return showNotificationsPanel()
}

function destroyLayout() {
  restoreStreamPanels()
  notificationsItem = null
  const host = document.getElementById("golden-layout-host")
  if (host) {
    delete host.dataset.glInit
  }
  if (resizeHandler) {
    window.removeEventListener("resize", resizeHandler)
    resizeHandler = null
  }
  if (layoutInstance) {
    try {
      layoutInstance.destroy()
    } catch (_) {
      /* already torn down */
    }
    layoutInstance = null
  }
  panelContainers.positions = null
  panelContainers.trades = null
  panelContainers.notifications = null
  updateNotificationsToggleButton()
}

function resizeLayout(host) {
  if (!layoutInstance || !host) {
    return
  }
  layoutInstance.setSize(host.clientWidth, host.clientHeight)
}

export function initLayout() {
  const host = document.getElementById("golden-layout-host")
  if (!host || !host.isConnected) {
    return false
  }

  cancelScheduledDestroy()
  destroyLayout()
  restoreStreamPanels()

  const mount = ensureMount(host)
  mount.innerHTML = ""

  const layout = new GoldenLayout(LAYOUT_CONFIG, mount)
  registerComponents(layout)
  layout.init()
  layoutInstance = layout

  attachStreamPanels()

  resizeHandler = () => resizeLayout(host)
  window.addEventListener("resize", resizeHandler)
  resizeLayout(host)

  host.dataset.glInit = "true"
  updateNotificationsToggleButton()
  bindLayoutToolbar()
  return true
}

function tryInit() {
  const host = document.getElementById("golden-layout-host")
  if (!host || !host.isConnected || host.dataset.glInit === "true") {
    return
  }
  initLayout()
}

function scheduleTryInit() {
  cancelScheduledDestroy()
  if (initTimer) {
    window.clearTimeout(initTimer)
  }
  initTimer = window.setTimeout(() => {
    initTimer = null
    requestAnimationFrame(() => {
      tryInit()
    })
  }, INIT_DEBOUNCE_MS)
}

function scheduleAttachPanels() {
  if (attachTimer) {
    window.clearTimeout(attachTimer)
  }
  attachTimer = window.setTimeout(() => {
    attachTimer = null
    if (document.getElementById("golden-layout-host")?.dataset.glInit === "true") {
      attachStreamPanels()
    }
  }, INIT_DEBOUNCE_MS)
}

window.antmanInitLayout = initLayout
window.antmanDestroyLayout = destroyLayout
window.antmanAttachPanels = attachStreamPanels
window.antmanToggleNotificationsPanel = toggleNotificationsPanel

function watchForLayoutHost() {
  const root = document.getElementById("hyper-app") || document.body
  const observer = new MutationObserver(() => {
    const host = document.getElementById("golden-layout-host")
    if (!host) {
      if (layoutInstance || panelContainers.positions) {
        scheduleDestroy()
      }
      return
    }
    cancelScheduledDestroy()
    bindLayoutToolbar()
    if (host.dataset.glInit !== "true") {
      scheduleTryInit()
    } else if (panelContainers.positions) {
      scheduleAttachPanels()
    }
  })
  observer.observe(root, { childList: true, subtree: true })
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    scheduleTryInit()
    bindLayoutToolbar()
    watchForLayoutHost()
  })
} else {
  scheduleTryInit()
  bindLayoutToolbar()
  watchForLayoutHost()
}
