(ns quanta.blotter-hyper.status.view
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.status.core :as core]
   [tick.core :as t]))

(def ^:private time-fmt (t/formatter "HH:mm:ss"))

(defn- fmt-server-time [instant]
  (when instant
    (t/format time-fmt (t/in instant t/UTC))))

(defn sse-connection-status
  "UTC clock + Hyper connection banner.
   Soft reconnect via h/reconnect when Datastar exhausts retries."
  []
  [:motion.div#sse-connection-status.sse-connection-status
   {:data-class (h/expr {:is-stale (not @h/connected?*)})}
   (h/reactive [core/server-time-a]
               (let [instant @core/server-time-a]
                 [:motion.span#sse-server-ts
                  {:data-server-ts (if instant (.toEpochMilli instant) 0)}
                  (fmt-server-time instant)]))
   [:motion.div.sse-interrupted
    "server connection interrupted"
    [:button {:type "button"
              :data-show (h/expr (or (= @h/connection* :error)
                                     (= @h/connection* :closed)))
              :data-on:click (h/reconnect)}
     "Retry"]]
   ;; Soft reconnect is handled by Hyper; keep script for now:
   #_[:script {:src "/js/sse-reconnect.js" :defer true}]])
