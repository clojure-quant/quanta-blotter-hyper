(ns quanta.blotter-hyper.status.view
  (:require
   [hyper.context :as ctx]
   [hyper.core :as h]
   [quanta.blotter-hyper.status.core :as core]
   [tick.core :as t]))

(def ^:private time-fmt (t/formatter "HH:mm:ss"))

(defn- fmt-server-time [instant]
  (when instant
    (t/format time-fmt (t/in instant t/UTC))))

(defn sse-connection-status
  "Banner when SSE timestamps stop updating for >2s.
   Stale detection + auto-reconnect via /js/sse-reconnect.js (loaded here)."
  []
  (let [tab-id (:hyper/tab-id ctx/*request*)]
    [:motion.div#sse-connection-status.sse-connection-status
     (when tab-id {:data-tab-id tab-id})
     (h/reactive [core/server-time-a]
                 (let [instant @core/server-time-a]
                   [:motion.span#sse-server-ts
                    {:data-server-ts (if instant (.toEpochMilli instant) 0)}
                    (fmt-server-time instant)]))
     [:motion.div.sse-interrupted "server connection interrupted"]
     [:script {:src "/js/sse-reconnect.js" :defer true}]]))
