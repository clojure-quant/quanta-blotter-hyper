



- admin panel
  - user management
  - shows connected browsers.
  - switch to view of trader.


time	acct	trader	acct name	camp	lbl	id	asset	D	qty	OT	lmt	status	wkg	fill	avg	Message
2026-07-01 14:24:46.795	1000	florian	pepperstone demo1	manual order	manual	MiIS3g	EURUSD	B	10000	LMT	1.1036	working	10000			—
2026-07-01 14:23:34.179	1000	florian	pepperstone demo1	manual order	manual	GgdZWH	EURUSD	B	10000	LMT	1.1035	working	10000			—
2026-07-01 14:23:31.148	1000	florian	pepperstone demo1	manual order	manual	KAxuPb	EURUSD	B	10000	LMT	1.1035	working	10000			—


 (h/reactive [sim/trades*]
      (ui/trades-table @sim/trades*))


      (ns antman.ui.panels
  (:require
   [hyper.core :as h]
   [antman.sim.state :as sim]
   [antman.ui.components :as ui]))

(defn register-panel!
  [_ panel-kw]
  (reset! (h/tab-cursor [:active-panels panel-kw] false) true))

(defn positions-panel
  [_req]
  (register-panel! nil :positions)
  (h/watch! sim/positions*)
  (h/reactive [sim/positions*]
    [:motion.div#panel-positions.panel-root
     (ui/sse-connection-status)
     [:h2 "Positions"]
     (ui/positions-table @sim/positions*)]))

(defn trades-panel
  [_req]
  (register-panel! nil :trades)
  (h/watch! sim/trades*)
  (h/reactive [sim/trades*]
    [:motion.div#panel-trades.panel-root
     [:h2 "Trades"]
     (ui/trades-table @sim/trades*)]))
