(ns playground.notification.view
  (:require
   [hyper.core :as h]
   [playground.nav :refer [nav]]
   [playground.notification.state :as notification-state]
   [playground.notification.simulate :as notifications]))

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
               {:data-on:click (h/action (notifications/mark-read! notification-state/notifications* notificationId))}
               "mark read"]]])]]])]))


(defn notifications-component []
  (h/reactive [notification-state/notifications*]
    [:motion.div#panel-notifications.panel-root
     (notifications-panel @notification-state/notifications*)]))

(defn notifications-page
  [_req]
  [:motion.div.notifications-page
   (nav)
   (notifications-component)])