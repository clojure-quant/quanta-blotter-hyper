(ns playground.golden-layout.page
  (:require
   [hyper.core :as h]
   [playground.notification.state :as notification-state]
   [playground.notification.view :as notification-view]
   [playground.nav :refer [nav]]))

(defn layout-page
  [_req]
  [:motion.div.layout-page
   (nav)
   [:motion.div.layout-toolbar
    [:button#toggle-notifications-btn.layout-btn
     {:type "button"
      :data-on:click "antmanToggleNotificationsPanel()"}
     "Show notifications"]]
   [:motion.div#golden-layout-host.golden-layout-host
    {:data-on-load "antmanInitLayout()"}
    [:motion.div#gl-mount]]
   [:motion.div#panel-stash.panel-stash
    (h/reactive [notification-state/notifications*]
      [:motion.div#panel-notifications.panel-root
       (notification-view/notifications-panel @notification-state/notifications*)])]])
