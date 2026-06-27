(ns antman.sim.notifications
  (:require
   [missionary.core :as m])
  (:import [java.time Instant]
           [java.time.format DateTimeFormatter]))

(defonce notif-seq* (atom 0))

(def symbols ["EURUSD" "GBPUSD" "USDJPY" "XAUUSD" "BTCUSD" "ETHUSD"])

(def templates
  [{:type "DispatchFailed"
    :severity "Error"
    :title "Broker dispatch failed"
    :message "Order for %s could not be dispatched. Manual review required."}
   {:type "RiskRejection"
    :severity "Warning"
    :title "Order rejected: %s"
    :message "Risk check failed. Review violations before resubmitting."}
   {:type "TradeOpened"
    :severity "Info"
    :title "Trade opened: %s"
    :message "Entry filled. Monitor stops and take-profit levels."}
   {:type "StopMoved"
    :severity "Info"
    :title "Stop loss updated"
    :message "Stop for %s moved to breakeven."}
   {:type "PartialExitTriggered"
    :severity "Info"
    :title "Partial exit triggered"
    :message "TP1 hit on %s. Remaining size reduced."}
   {:type "BrokerDisconnected"
    :severity "Error"
    :title "Broker connection lost"
    :message "MT4 gateway for %s is unreachable. Reconnect required."}])

(defn- rand-elt [coll]
  (nth coll (rand-int (count coll))))

(defn- today-trade-id [symbol]
  (let [date (.format (java.time.LocalDate/now)
                      (DateTimeFormatter/ofPattern "yyyyMMdd"))]
    (str symbol "-" date "-" (format "%03d" (inc (rand-int 999))))))

(defn- next-notification-id []
  (str "notif-" (format "%03d" (swap! notif-seq* inc))))

(defn notification
  []
  (let [{:keys [type severity title message]} (rand-elt templates)
        symbol (rand-elt symbols)]
    {:notificationId (next-notification-id)
     :type type
     :title (format title symbol)
     :message (format message symbol)
     :tradeId (today-trade-id symbol)
     :severity severity
     :read false
     :createdAt (str (Instant/now))}))

(defn mark-read!
  [notifications* notification-id]
  (swap! notifications*
         (fn [notifs]
           (mapv #(if (= (:notificationId %) notification-id)
                    (assoc % :read true)
                    %)
                 notifs))))

(defn unread
  [notifications]
  (filterv #(not (:read %)) notifications))

(defn notifications-flow
  [notifications*]
  (m/ap
    (m/?> (m/seed (repeat nil)))
    (m/? (m/sp (swap! notifications* conj (notification))))
    (m/? (m/sleep (+ 3000 (rand-int 4000))))))

(defn start!
  [notifications*]
  (let [flow (notifications-flow notifications*)
        task (m/reduce (fn [_ _] nil) nil flow)
        dispose! (task (constantly nil) #(prn "notifications sim error:" %))]
    dispose!))
