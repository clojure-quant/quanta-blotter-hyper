(ns antman.sim.trades
  (:require
   [antman.sim.generate :as gen]
   [missionary.core :as m]))

(defn trades-flow
  [trades*]
  (m/ap
    (m/?> (m/seed (repeat nil)))
    (m/? (m/sp (swap! trades* conj (gen/trade))))
    (m/? (m/sleep (+ 1000 (rand-int 2000))))))

(defn start!
  [trades*]
  (let [flow (trades-flow trades*)
        task (m/reduce (fn [_ _] nil) nil flow)
        dispose! (task (constantly nil) #(prn "trades sim error:" %))]
    dispose!))
