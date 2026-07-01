(ns demo.handler
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.handler :refer [head-tags]]
   [antman.sim.state :as sim]
   [antman.sse.heartbeat :as heartbeat]
   ))

(defn create-handler
  "Ring handler for Hyper. Optional clip refs establish service start order."
  [routes]
  (h/create-handler
   routes
   :static-resources "public"
   :head #'head-tags
   :middleware [auth/wrap-hydrate-identity]
   :watches [#'sim/notifications* #'heartbeat/server-ts*]))
