(ns quanta.blotter-hyper.status.core
  "Server instant pushed over SSE (~1 Hz) for connection health checks."
  (:require
   [missionary.core :as m]
   [tick.core :as t]))

(def server-time-a (atom nil))

(defonce heartbeat-disposer* (atom nil))

(defn- heartbeat-flow
  []
  (m/ap
    (m/?> (m/seed (repeat nil)))
    (reset! server-time-a (t/now))
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
