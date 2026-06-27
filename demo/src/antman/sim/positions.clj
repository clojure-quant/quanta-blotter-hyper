(ns antman.sim.positions
  (:require
   [missionary.core :as m]))

(defn positions-flow
  [positions* tick-fn]
  (m/ap
    (m/?> (m/seed (repeat nil)))
    (m/? (m/sp (swap! positions* tick-fn)))
    (m/? (m/sleep 2000))))

(defn start!
  [positions* tick-fn]
  (let [flow (positions-flow positions* tick-fn)
        task (m/reduce (fn [_ _] nil) nil flow)
        dispose! (task (constantly nil) #(prn "positions sim error:" %))]
    dispose!))
