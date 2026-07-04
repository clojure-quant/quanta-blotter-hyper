



(defn register-panel!
  [_ panel-kw]
  (reset! (h/tab-cursor [:active-panels panel-kw] false) true))

(defn positions-panel
  [_req]
  (register-panel! nil :positions)
  (h/watch! sim/positions*)
  (h/reactive [sim/positions*]
    [:motion.div#panel-positions.panel-root
     (sse-connection-status)
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
