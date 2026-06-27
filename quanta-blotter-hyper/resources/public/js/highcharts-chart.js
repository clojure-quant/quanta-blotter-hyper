import Highcharts from "https://cdn.jsdelivr.net/npm/highcharts@12.2.0/+esm"

const DEFAULT_UPDATE_ARGS = [true, true, true]

/**
 * <highcharts-chart> — Highcharts wrapper without framework dependencies.
 * API mirrors https://github.com/ashubham/highcharts-webcomponent
 */
class HighchartsChart extends HTMLElement {
  constructor() {
    super()
    this._options = null
    this._chart = null
    this._highcharts = Highcharts
    this._updateArgs = DEFAULT_UPDATE_ARGS
    this._container = document.createElement("div")
    this._container.className = "highcharts-chart-container"
    this._container.style.height = "100%"
    this._container.style.width = "100%"
  }

  static get observedAttributes() {
    return ["constructor-type", "allow-chart-update", "immutable"]
  }

  get constructorType() {
    return this.getAttribute("constructor-type") || "chart"
  }

  set constructorType(value) {
    this.setAttribute("constructor-type", value || "chart")
  }

  get allowChartUpdate() {
    return this.getAttribute("allow-chart-update") !== "false"
  }

  set allowChartUpdate(value) {
    if (value === false || value === "false") {
      this.setAttribute("allow-chart-update", "false")
    } else {
      this.removeAttribute("allow-chart-update")
    }
  }

  get immutable() {
    return this.hasAttribute("immutable")
  }

  set immutable(value) {
    if (value) {
      this.setAttribute("immutable", "")
    } else {
      this.removeAttribute("immutable")
    }
  }

  get highcharts() {
    return this._highcharts
  }

  set highcharts(value) {
    this._highcharts = value || Highcharts
  }

  get options() {
    return this._options
  }

  set options(value) {
    this._options = value
    this._applyOptions()
  }

  get updateArgs() {
    return this._updateArgs
  }

  set updateArgs(value) {
    this._updateArgs = Array.isArray(value) ? value : DEFAULT_UPDATE_ARGS
  }

  get chart() {
    return this._chart
  }

  connectedCallback() {
    if (!this.contains(this._container)) {
      this.appendChild(this._container)
    }
    if (this._options && !this._chart) {
      this._createChart()
    }
  }

  disconnectedCallback() {
    this._destroyChart()
  }

  attributeChangedCallback() {
    if (this._chart && this.immutable) {
      this._createChart()
    }
  }

  _destroyChart() {
    if (this._chart) {
      this._chart.destroy()
      this._chart = null
    }
  }

  _applyOptions() {
    if (!this._options || !this.isConnected) {
      return
    }
    if (!this.allowChartUpdate) {
      return
    }
    if (this.immutable || !this._chart) {
      this._createChart()
      return
    }
    this._chart.update(this._options, ...this._updateArgs)
  }

  _createChart() {
    const H = this._highcharts || window.Highcharts
    const ctor = this.constructorType

    if (!H) {
      console.warn('highcharts-chart: no Highcharts instance (set "highcharts" or load Highcharts)')
      return
    }
    if (!H[ctor]) {
      console.warn(`highcharts-chart: unknown constructorType "${ctor}"`)
      return
    }
    if (!this._options) {
      console.warn('highcharts-chart: "options" is required')
      return
    }

    this._destroyChart()
    // Assign before "load" — Highcharts may invoke the callback synchronously,
    // so reading this._chart inside the callback can still be undefined.
    this._chart = H[ctor](this._container, this._options)
    this.dispatchEvent(
      new CustomEvent("load", { detail: this._chart, bubbles: true }),
    )
  }
}

if (!customElements.get("highcharts-chart")) {
  customElements.define("highcharts-chart", HighchartsChart)
}

export { HighchartsChart, Highcharts }
