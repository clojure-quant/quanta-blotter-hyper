import "./highcharts-chart.js?v=2"

const CHART_COUNT = 12
const MAX_POINTS = 180
const TICK_MS = 1000

/** @type {number[]} */
let activeTimers = []
/** @type {HTMLElement | null} */
let activeGrid = null
/** Bumped on destroy so stale "load" handlers do not start tickers. */
let initSession = 0
/** Debounce teardown while Hyper morphs between routes. */
let destroyTimer = null
/** Coalesce init attempts while SSE / layout mutations fire. */
let initTimer = null
const DESTROY_DELAY_MS = 200
const INIT_DEBOUNCE_MS = 60

function randomWalk(count, start = 100) {
  const data = []
  let price = start
  for (let i = 0; i < count; i++) {
    price += (Math.random() - 0.5) * 2
    data.push(Number(price.toFixed(2)))
  }
  return data
}

function chartOptions(title, data) {
  return {
    chart: {
      animation: false,
      backgroundColor: "transparent",
      margin: [4, 4, 18, 4],
    },
    credits: { enabled: false },
    legend: { enabled: false },
    title: {
      text: title,
      style: { color: "#a8b4c8", fontSize: "11px" },
      margin: 2,
    },
    xAxis: {
      labels: { enabled: false },
      tickLength: 0,
      lineColor: "#333",
    },
    yAxis: {
      title: { text: null },
      labels: { style: { color: "#888", fontSize: "9px" } },
      gridLineColor: "#2a2a3e",
    },
    tooltip: { enabled: false },
    plotOptions: {
      series: {
        animation: false,
        enableMouseTracking: false,
        marker: { enabled: false },
        lineWidth: 1,
        states: { hover: { enabled: false } },
      },
    },
    series: [
      {
        name: "Price",
        data,
        color: "#7eb8ff",
      },
    ],
  }
}

function stopTimer(id) {
  window.clearInterval(id)
  const idx = activeTimers.indexOf(id)
  if (idx >= 0) {
    activeTimers.splice(idx, 1)
  }
}

function stopAllTickers() {
  for (const id of activeTimers) {
    window.clearInterval(id)
  }
  activeTimers = []
}

function firstSeries(chart) {
  if (!chart || !Array.isArray(chart.series) || chart.series.length === 0) {
    return null
  }
  return chart.series[0]
}

function startTicker(host, seedPrice) {
  let price = seedPrice
  const id = window.setInterval(() => {
    if (!host.isConnected) {
      stopTimer(id)
      return
    }
    const series = firstSeries(host.chart)
    if (!series) {
      stopTimer(id)
      return
    }
    price += (Math.random() - 0.5) * 2
    const shift = series.data.length >= MAX_POINTS
    series.addPoint(Number(price.toFixed(2)), true, shift)
  }, TICK_MS)
  activeTimers.push(id)
  return id
}

function gridNeedsInit(grid) {
  if (grid.dataset.initializing === "true") {
    return false
  }
  return (
    grid.dataset.initialized !== "true" || grid.children.length < CHART_COUNT
  )
}

function scheduleDestroy() {
  if (destroyTimer) {
    window.clearTimeout(destroyTimer)
  }
  destroyTimer = window.setTimeout(() => {
    destroyTimer = null
    if (!document.getElementById("charts-grid")) {
      destroyHighchartsRandom()
    }
  }, DESTROY_DELAY_MS)
}

function cancelScheduledDestroy() {
  if (destroyTimer) {
    window.clearTimeout(destroyTimer)
    destroyTimer = null
  }
}

function destroyHighchartsRandom() {
  if (initTimer) {
    window.clearTimeout(initTimer)
    initTimer = null
  }
  if (!activeGrid && activeTimers.length === 0) {
    return
  }
  initSession += 1
  stopAllTickers()
  const grid = activeGrid
  if (grid) {
    grid.replaceChildren()
    delete grid.dataset.initialized
    delete grid.dataset.initializing
  }
  activeGrid = null
}

function initHighchartsRandom({ force = false } = {}) {
  const grid = document.getElementById("charts-grid")
  if (!grid || !grid.isConnected) {
    return
  }

  cancelScheduledDestroy()

  if (activeGrid && activeGrid !== grid) {
    destroyHighchartsRandom()
  }
  if (!force && !gridNeedsInit(grid)) {
    return
  }

  delete grid.dataset.initialized
  grid.dataset.initializing = "true"
  stopAllTickers()
  const session = initSession
  grid.replaceChildren()
  activeGrid = grid

  for (let i = 0; i < CHART_COUNT; i++) {
    const data = randomWalk(MAX_POINTS, 100 + i * 3)
    const el = document.createElement("highcharts-chart")
    el.options = chartOptions(`Chart ${i + 1}`, data)
    el.addEventListener(
      "load",
      (event) => {
        if (session !== initSession) {
          return
        }
        const host = event.currentTarget
        const series = firstSeries(event.detail ?? host.chart)
        if (!series) {
          return
        }
        const points = series.data
        const last = points.length > 0 ? points[points.length - 1] : null
        const seed = last?.y ?? data[data.length - 1]
        startTicker(host, seed)
      },
      { once: true },
    )
    grid.appendChild(el)
  }

  delete grid.dataset.initializing
  grid.dataset.initialized = "true"

  if (grid.children.length < CHART_COUNT) {
    delete grid.dataset.initialized
    scheduleTryInit()
  }
}

function tryInit() {
  const grid = document.getElementById("charts-grid")
  if (!grid || !grid.isConnected) {
    return
  }
  if (!gridNeedsInit(grid)) {
    return
  }
  initHighchartsRandom()
}

function scheduleTryInit() {
  cancelScheduledDestroy()
  if (initTimer) {
    window.clearTimeout(initTimer)
  }
  initTimer = window.setTimeout(() => {
    initTimer = null
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        tryInit()
      })
    })
  }, INIT_DEBOUNCE_MS)
}

function watchForGrid() {
  const root = document.getElementById("hyper-app") || document.body
  const observer = new MutationObserver(() => {
    const grid = document.getElementById("charts-grid")
    if (!grid) {
      if (activeGrid || activeTimers.length > 0) {
        scheduleDestroy()
      }
      return
    }
    scheduleTryInit()
  })
  observer.observe(root, { childList: true, subtree: true })
}

window.antmanInitHighchartsRandom = () => initHighchartsRandom({ force: true })
window.antmanDestroyHighchartsRandom = destroyHighchartsRandom

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    scheduleTryInit()
    watchForGrid()
  })
} else {
  scheduleTryInit()
  watchForGrid()
}
