(ns quanta.blotter-hyper.trader.live
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter-hyper.nav :as nav]
   [quanta.blotter-hyper.trader.send-order :as send-order]
   [quanta.blotter-hyper.view.orders :as orders-view]
   [quanta.blotter-hyper.view.positions :as positions-view]))

(defn- filter-for-trader [trader]
  (map (fn [{:keys [open-positions working-orders]}]
         {:open-positions (into [] (filter #(= (:position/trader %) trader) open-positions))
          :working-orders (into [] (filter #(= (:order/trader %) trader) working-orders))})))

(defn- trader-live-flow [trader trading-state-trader]
  (m/eduction
   (filter-for-trader trader)
   (m/watch trading-state-trader)))

(defn- process-trader-live-f [trader trading-state-trader data-a]
  (m/ap
   (let [data (m/?> (trader-live-flow trader trading-state-trader))]
     (reset! data-a data)
     nil)))

(defn- start-trader-live-processor [trader trading-state-trader data-a]
  (let [f (process-trader-live-f trader trading-state-trader data-a)
        t (m/reduce (fn [_r _v] nil) nil f)]
    (t #(println "trader live processor done" %)
       #(println "trader live processor error" %))))

(defn live-page
  [{:keys [hyper/env] :as _req}]
  (h/view
   {:mount (fn []
             (let [db (:db env)
                   oms (get-in env [:oms-server :oms])
                   _ (println "*** live-page ctx: " (keys env))
                   _ (assert db ":db needs to be in :ctx")
                   _ (assert oms ":oms-server :oms needs to be in :ctx")
                   ts (get-in env [:oms-server :trading-state-trader])
                   _ (assert ts ":oms-server :trading-state-trader needs to be in :ctx")
                   identity @(h/session-cursor :identity)
                   trader (name (:user identity))
                   accounts (send-order/trader-accounts db trader)
                   first-account (ffirst accounts)
                   data-a (atom nil)
                   order-state-a (atom (send-order/default-state first-account))
                   order-error-a (atom nil)
                   this {:data-a data-a
                         :order-state-a order-state-a
                         :order-error-a order-error-a
                         :accounts accounts
                         :assets (send-order/available-assets)
                         :oms oms
                         :trader trader
                         :dispose! (start-trader-live-processor trader ts data-a)}]
               (h/watch! data-a)
               (h/watch! order-state-a)
               (h/watch! order-error-a)
               this))
    :render (fn [{:keys [data-a order-state-a order-error-a accounts assets oms]} _req]
              (let [{:keys [open-positions working-orders]}
                    (or @data-a {:open-positions [] :working-orders []})]
                [:motion.div.live-page
                 (nav/trader-nav)
                 [:div.live-layout
                  [:div.live-main
                   [:header.live-header
                    [:h1 "Live"]]
                   [:div.live-columns
                    [:section.live-column
                     [:h2 "Open positions"]
                     (positions-view/positions-table open-positions)]
                    [:section.live-column
                     [:h2 "Working orders"]
                     (orders-view/orders-table working-orders
                                               {:oms oms
                                                :error-a order-error-a})]]]
                  (send-order/panel {:state-a order-state-a
                                     :error-a order-error-a
                                     :accounts accounts
                                     :assets assets
                                     :oms oms})]]))
    :unmount (fn [{:keys [dispose!]}]
               (dispose!))}))
