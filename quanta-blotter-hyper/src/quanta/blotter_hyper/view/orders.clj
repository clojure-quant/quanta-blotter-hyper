(ns quanta.blotter-hyper.view.orders
  (:require
   [datahike.api :refer [q]]
   [quanta.blotter-hyper.view.accounts :as accounts-view]
   [quanta.blotter-hyper.view.common :as common]))

(defn- order-type-cell [order-type]
  (case order-type
    :stop [:td.ot "STP"]
    :limit [:td.ot "LMT"]
    :market [:td.ot "MKT"]
    [:td.ot (common/fmt-cell order-type)]))

(defn- wkg-cell [qty-working]
  [:td {:class (str "wkg" (when (common/pos-num? qty-working) " wkg-active"))}
   (common/fmt-cell qty-working)])

(defn- status-cell [status]
  [:td {:class (str "status"
                    (case status
                      :filled " status-filled"
                      :rejected " status-muted"
                      :cancelled " status-muted"
                      :working " status-working"
                      ""))}
   (common/fmt-cell status)])

(defn orders-table
  [orders]
  (let [orders (sort-by :order/date #(compare %2 %1) orders)]
    [:div.orders-table-wrap
     [:table.orders-table
      [:thead
       [:tr
        [:th.time "time"]
        [:th "acct"]
        [:th "trader"]
        [:th "acct name"]
        [:th "camp"]
        [:th "lbl"]
        [:th "id"]
        [:th "asset"]
        [:th.side-col "D"]
        [:th.num "qty"]
        [:th "OT"]
        [:th "lmt"]
        [:th "status"]
        [:th "wkg"]
        [:th.num "fill"]
        [:th.num "avg"]
        [:th "Message"]]]
      [:tbody
       (if (empty? orders)
         [:tr [:td {:colspan 17} "No orders"]]
         (for [order orders]
           [:tr {:key (:order/id order)}
            [:td.time (common/fmt-instant-utc (:order/date order))]
            [:td (common/fmt-cell (:order/account-id order))]
            [:td (common/fmt-cell (:order/trader order))]
            [:td (common/fmt-cell (:order/account-name order))]
            [:td (common/fmt-cell (:order/campaign order))]
            [:td (common/fmt-cell (:order/label order))]
            [:td (common/fmt-cell (:order/id order))]
            [:td (common/fmt-cell (:order/asset order))]
            (common/side-cell (:order/side order))
            [:td.num (common/fmt-cell (:order/qty order))]
            (order-type-cell (:order/type order))
            [:td (common/fmt-cell (:order/limit order))]
            (status-cell (:order/status order))
            (wkg-cell (:order/qty-working order))
            [:td.num (common/fmt-pos-num (:order/qty-filled order))]
            [:td.num (when-let [avg (:order/avg-price order)] (str avg))]
            [:td (common/fmt-cell (:order/text order))]]))]]]))

(defn- enrich-order [order]
  (common/enrich-account-fields order
                                :order/account-db
                                :order/account-name
                                :order/trader))

(defn query-orders-by-account-pred [conn account-id-pred]
  (->> (q '[:find [(pull ?e [* {:order/account-db [:account/name :account/trader]}]) ...]
             :in $ ?account-id-pred
             :where
             [?e :order/account-id ?account-id]
             [(?account-id-pred ?account-id)]
             [?e :order/id _]]
          @conn account-id-pred)
       (mapv enrich-order)))

(defn query-all-orders [conn]
  (query-orders-by-account-pred conn (constantly true)))

(defn query-account-orders [conn account-id]
  (->> (q '[:find [(pull ?e [* {:order/account-db [:account/name :account/trader]}]) ...]
             :in $ ?account-id
             :where
             [?e :order/account-id ?account-id]
             [?e :order/id _]]
          @conn account-id)
       (mapv enrich-order)))

(defn query-orders [conn {:keys [account-id trader] :as opts}]
  (cond
    account-id (query-account-orders conn account-id)
    (contains? opts :trader) (query-orders-by-account-pred conn (accounts-view/account-id-pred conn trader))
    :else (query-all-orders conn)))
