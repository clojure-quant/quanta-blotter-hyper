(ns antman.ui.components
  (:require
   [hyper.context :as ctx]
   [hyper.core :as h]
   [antman.sim.notifications :as notifications]
   [antman.sim.state :as sim]
   [antman.sse.heartbeat :as heartbeat]))

(defn- severity-class [severity]
  (case severity
    "Error" "severity-error"
    "Warning" "severity-warning"
    "Info" "severity-info"
    "severity-info"))

(defn notifications-panel
  [notifications]
  (let [unread (notifications/unread notifications)]
    [:div.notifications-panel
     [:h2.notifications-heading "Notifications"]
     (if (empty? unread)
       [:p.notifications-empty "No unread notifications"]
       [:motion.div.notifications-table-wrap
        [:table.notifications-table
         [:thead
          [:tr
           [:th "Time"]
           [:th "Severity"]
           [:th "Type"]
           [:th "Title"]
           [:th "Message"]
           [:th "Trade"]
           [:th ""]]]
         [:tbody
          (for [{:keys [notificationId type title message tradeId severity createdAt]}
                unread]
            [:tr {:key notificationId
                  :class (severity-class severity)}
             [:td.time createdAt]
             [:td.severity severity]
             [:td.type type]
             [:td.title title]
             [:td.message message]
             [:td.trade-id tradeId]
             [:td.actions
              [:button.mark-read-btn
               {:data-on:click (h/action (notifications/mark-read! sim/notifications* notificationId))}
               "mark read"]]])]]])]))

(defn sse-connection-status
  "Banner when SSE timestamps stop updating for >2s.
   Stale detection + auto-reconnect live in /js/sse-reconnect.js."
  []
  (let [tab-id (:hyper/tab-id ctx/*request*)]
    [:motion.div#sse-connection-status.sse-connection-status
     (when tab-id {:data-tab-id tab-id})
     (h/reactive [heartbeat/server-ts*]
                 [:motion.span#sse-server-ts
                  {:data-server-ts @heartbeat/server-ts*
                   :hidden true}])
     [:motion.div.sse-interrupted "server connection interrupted"]]))

(defn nav
  []
  [:nav.app-nav
   [:a (h/navigate :layout) "Layout"]
   " · "
   [:a (h/navigate :highcharts-random) "Highcharts random"]
   " · "
   [:a (h/navigate :simulator) "Simulator"]
   " · "
   [:a {:href "/me"} "User"]])
