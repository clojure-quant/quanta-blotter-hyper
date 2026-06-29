(ns quanta.blotter-hyper.trader.live
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter-hyper.nav :as nav]
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
  [{:keys [ctx] :as _req}]
  (h/view
   {:mount (fn []
             (let [ts (get-in ctx [:oms-server :trading-state-trader])
                   _ (assert ts ":oms-server :trading-state-trader needs to be in :ctx")
                   identity @(h/session-cursor :identity)
                   trader (name (:user identity))
                   data-a (atom nil)
                   this {:data-a data-a
                         :trader trader
                         :dispose! (start-trader-live-processor trader ts data-a)}]
               (h/watch! data-a)
               this))
    :render (fn [{:keys [data-a trader]} _req]
              (let [{:keys [open-positions working-orders]}
                    (or @data-a {:open-positions [] :working-orders []})]
                [:motion.div.live-page
                 (nav/nav)
                 [:header.live-header
                  [:h1 "Live"]
                  [:span.live-trader (str "Trader: " trader)]]
                 [:div.live-columns
                  [:section.live-column
                   [:h2 "Open positions"]
                   (positions-view/positions-table open-positions)]
                  [:section.live-column
                   [:h2 "Working orders"]
                   (orders-view/orders-table working-orders)]]]))
    :unmount (fn [{:keys [dispose!]}]
               (dispose!))}))
