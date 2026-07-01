(ns antman.ui.layout-page
  (:require
   [hyper.core :as h]
   [antman.sim.state :as sim]
   [antman.ui.components :as ui]))

(defn layout-page
  [_req]
  [:motion.div.layout-page
   (ui/nav)
   [:motion.div.layout-toolbar
    [:button#toggle-notifications-btn.layout-btn
     {:type "button"
      :data-on:click "antmanToggleNotificationsPanel()"}
     "Show notifications"]]
   (ui/sse-connection-status)
   [:motion.div#golden-layout-host.golden-layout-host
    {:data-on-load "antmanInitLayout()"}
    [:motion.div#gl-mount]]
   [:motion.div#panel-stash.panel-stash
    (h/reactive [sim/notifications*]
      [:motion.div#panel-notifications.panel-root
       (ui/notifications-panel @sim/notifications*)])]])
