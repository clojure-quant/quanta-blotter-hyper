(ns antman.sim.state
  (:require
   [antman.sim.generate :as gen]
   [antman.sim.notifications :as notifications-sim]
   [antman.sim.positions :as positions-sim]
   [antman.sim.trades :as trades-sim]))

(def positions* (atom (gen/seed-positions 5)))
(def trades* (atom []))
(def notifications* (atom []))

(defonce sim-disposers* (atom {}))

(defn- clear-price-flashes
  [positions]
  (mapv #(dissoc % :price-flash) positions))

(defn apply-position-tick!
  "Update a random position's price/pl, or add a new one occasionally."
  [positions]
  (let [positions (clear-price-flashes positions)]
    (if (and (seq positions) (< (rand) 0.85))
      (let [idx (rand-int (count positions))
            pos (nth positions idx)]
        (assoc positions idx (gen/position-update pos)))
      (conj (or positions []) (gen/position)))))

(defn start!
  []
  (when-not (:positions @sim-disposers*)
    (reset! sim-disposers*
            {:positions (positions-sim/start! positions* apply-position-tick!)
             :trades (trades-sim/start! trades*)
             :notifications (notifications-sim/start! notifications*)}))
  :started)

(defn stop!
  []
  (doseq [[_ dispose!] @sim-disposers*]
    (dispose!))
  (reset! sim-disposers* {})
  :stopped)
