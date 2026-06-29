(ns quanta.blotter-hyper.admin.live
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.nav :as nav]
   [quanta.blotter-hyper.view.orders :as orders-view]
   [quanta.blotter-hyper.view.positions :as positions-view]))

(defn live-page
  [{:keys [ctx] :as _req}]
  (h/view
   {:mount (fn []
             (let [ts (get-in ctx [:oms-server :trading-state-trader])
                   _ (assert ts ":oms-server :trading-state-trader needs to be in :ctx")]
               (h/watch! ts)
               {:trading-state-trader ts}))
    :render (fn [{:keys [trading-state-trader]} _req]
              (let [{:keys [open-positions working-orders]} @trading-state-trader]
                [:motion.div.live-page
                 (nav/admin-nav)
                 [:header.live-header
                  [:h1 "Live"]]
                 [:div.live-columns
                  [:section.live-column
                   [:h2 "Open positions"]
                   (positions-view/positions-table open-positions)]
                  [:section.live-column
                   [:h2 "Working orders"]
                   (orders-view/orders-table working-orders)]]]))}))
