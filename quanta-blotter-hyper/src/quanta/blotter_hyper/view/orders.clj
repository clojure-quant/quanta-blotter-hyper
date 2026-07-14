(ns quanta.blotter-hyper.view.orders
  (:require
   [datahike.api :refer [q]]
   [hyper.core :as h]
   [quanta.blotter-hyper.trader.send-order :as send-order]
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

(defn- cancel-order-cell [order {:keys [oms error-a]}]
  [:td
   [:button.send-order-cancel
    {:type "button"
     :data-on:click (h/action (send-order/cancel-by-order! oms order error-a))}
    "Cancel"]])

(defn orders-table
  ([orders] (orders-table orders nil))
  ([orders cancel-opts]
   (let [orders (sort-by :order/date #(compare %2 %1) orders)
         cancel? (some? cancel-opts)
         col-count (if cancel? 18 17)]
     [:div.orders-table-wrap
      [:table.orders-table
       [:thead
        [:tr
         [:th.time "time"]
         [:th "trader"]
         [:th "acct"]
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
         [:th "Message"]
         (when cancel? [:th ""])]]
       [:tbody
        (if (empty? orders)
          [:tr [:td {:colspan col-count} "No orders"]]
          (for [order orders]
            [:tr {:key (:order/id order)}
             [:td.time (common/fmt-instant-utc (:order/date order))]
             [:td (common/fmt-cell (:order/trader order))]
             [:td (common/fmt-cell (:order/account-id order))]
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
             [:td (common/fmt-cell (:order/text order))]
             (when cancel? (cancel-order-cell order cancel-opts))]))]]])))

(defn- enrich-order [order]
  (common/enrich-account-fields order
                                :order/account-db
                                :order/account-name
                                :order/trader))

(defn- campaign-pred
  "Substring match on campaign; used as a Datahike predicate."
  [campaign]
  (common/substring-pred campaign))

(defn- asset-pred
  [asset]
  (common/substring-pred asset))

(defn- date-pred
  [start-date]
  (common/start-date-pred start-date))

(defn query-orders-by-account-pred
  ([conn account-id-pred]
   (query-orders-by-account-pred conn account-id-pred nil nil nil))
  ([conn account-id-pred campaign]
   (query-orders-by-account-pred conn account-id-pred campaign nil nil))
  ([conn account-id-pred campaign asset]
   (query-orders-by-account-pred conn account-id-pred campaign asset nil))
  ([conn account-id-pred campaign asset start-date]
   (->> (if (seq campaign)
          (q '[:find [(pull ?e [* {:order/account-db [:account/name :account/trader]}]) ...]
               :in $ ?account-id-pred ?campaign-pred ?asset-pred ?date-pred
               :where
               [?e :order/account-id ?account-id]
               [(?account-id-pred ?account-id)]
               [?e :order/campaign ?c]
               [(?campaign-pred ?c)]
               [?e :order/asset ?asset]
               [(?asset-pred ?asset)]
               [?e :order/date ?d]
               [(?date-pred ?d)]
               [?e :order/id _]]
              @conn account-id-pred (campaign-pred campaign) (asset-pred asset) (date-pred start-date))
          (q '[:find [(pull ?e [* {:order/account-db [:account/name :account/trader]}]) ...]
               :in $ ?account-id-pred ?asset-pred ?date-pred
               :where
               [?e :order/account-id ?account-id]
               [(?account-id-pred ?account-id)]
               [?e :order/asset ?asset]
               [(?asset-pred ?asset)]
               [?e :order/date ?d]
               [(?date-pred ?d)]
               [?e :order/id _]]
              @conn account-id-pred (asset-pred asset) (date-pred start-date)))
        (mapv enrich-order))))

(defn query-all-orders
  ([conn] (query-all-orders conn nil nil nil))
  ([conn campaign] (query-all-orders conn campaign nil nil))
  ([conn campaign asset] (query-all-orders conn campaign asset nil))
  ([conn campaign asset start-date]
   (query-orders-by-account-pred conn (constantly true) campaign asset start-date)))

(defn query-account-orders
  ([conn account-id]
   (query-account-orders conn account-id nil nil nil))
  ([conn account-id campaign]
   (query-account-orders conn account-id campaign nil nil))
  ([conn account-id campaign asset]
   (query-account-orders conn account-id campaign asset nil))
  ([conn account-id campaign asset start-date]
   (->> (if (seq campaign)
          (q '[:find [(pull ?e [* {:order/account-db [:account/name :account/trader]}]) ...]
               :in $ ?account-id ?campaign-pred ?asset-pred ?date-pred
               :where
               [?e :order/account-id ?account-id]
               [?e :order/campaign ?c]
               [(?campaign-pred ?c)]
               [?e :order/asset ?a]
               [(?asset-pred ?a)]
               [?e :order/date ?d]
               [(?date-pred ?d)]
               [?e :order/id _]]
              @conn account-id (campaign-pred campaign) (asset-pred asset) (date-pred start-date))
          (q '[:find [(pull ?e [* {:order/account-db [:account/name :account/trader]}]) ...]
               :in $ ?account-id ?asset-pred ?date-pred
               :where
               [?e :order/account-id ?account-id]
               [?e :order/asset ?a]
               [(?asset-pred ?a)]
               [?e :order/date ?d]
               [(?date-pred ?d)]
               [?e :order/id _]]
              @conn account-id (asset-pred asset) (date-pred start-date)))
        (mapv enrich-order))))

(defn query-orders [conn {:keys [account-id trader campaign asset start-date] :as opts}]
  (let [trader-pred (if (contains? opts :trader)
                      (accounts-view/account-id-pred conn trader)
                      (constantly true))
        pred (common/account-filter-pred trader-pred account-id)]
    (query-orders-by-account-pred conn pred campaign asset start-date)))
