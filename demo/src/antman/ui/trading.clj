(ns antman.ui.trading
  (:require
   [hyper.core :as h]
   [antman.sim.state :as sim]
   [antman.ui.components :as ui]))

(defn trading-page
  [_req]
  [:motion.div.trading-page
   (ui/nav)
   [:h1 "Trading"]
   [:section
    [:h2 "Positions"]
    (h/reactive [sim/positions*]
      (ui/positions-table @sim/positions*))]
   [:section
    [:h2 "Trades"]
    (h/reactive [sim/trades*]
      (ui/trades-table @sim/trades*))]])
