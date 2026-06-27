(ns antman.sse.heartbeat
  "Server timestamp pushed over SSE (~1 Hz) for connection health checks."
  (:require
   [missionary.core :as m]))

(def server-ts* (atom 0))

(defonce heartbeat-disposer* (atom nil))

(defn- heartbeat-flow
  []
  (m/ap
    (m/?> (m/seed (repeat nil)))
    (reset! server-ts* (System/currentTimeMillis))
    (m/? (m/sleep 1000))))

(defn start!
  []
  (when-not @heartbeat-disposer*
    (reset! heartbeat-disposer*
            (let [flow (heartbeat-flow)
                  task (m/reduce (fn [_ _] nil) nil flow)]
              (task (constantly nil) #(prn "sse heartbeat error:" %)))))
  :started)

(defn stop!
  []
  (when-let [dispose! @heartbeat-disposer*]
    (dispose!)
    (reset! heartbeat-disposer* nil))
  :stopped)
