(ns playground.notification.state
  (:require
   [playground.notification.simulate :as notifications-sim]))

(def notifications* (atom []))

(defonce sim-disposers* (atom {}))

(defn start!
  []
  (when-not (:notifications @sim-disposers*)
    (reset! sim-disposers*
            {:notifications (notifications-sim/start! notifications*)}))
  :started)

(defn stop!
  []
  (doseq [[_ dispose!] @sim-disposers*]
    (dispose!))
  (reset! sim-disposers* {})
  :stopped)
