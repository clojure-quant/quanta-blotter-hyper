(ns quanta.blotter-hyper.view.trades
  (:require
   [datahike.api :refer [q]]
   [quanta.blotter-hyper.view.accounts :as accounts-view]
   [quanta.blotter-hyper.view.common :as common]))

(defn fill-table
  [fills]
  (let [fills (sort-by :fill/date #(compare %2 %1) fills)]
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
        [:th "order-id"]
        [:th "asset"]
        [:th.side-col "D"]
        [:th.num "qty"]
        [:th.num "price"]
        [:th "id"]]]
      [:tbody
       (if (empty? fills)
         [:tr [:td {:colspan 12} "No trades"]]
         (for [fill fills]
           [:tr {:key (:fill/id fill)}
            [:td.time (common/fmt-instant-utc (:fill/date fill))]
            [:td (common/fmt-cell (:fill/account-id fill))]
            [:td (common/fmt-cell (:fill/trader fill))]
            [:td (common/fmt-cell (:fill/account-name fill))]
            [:td (common/fmt-cell (:fill/campaign fill))]
            [:td (common/fmt-cell (:fill/label fill))]
            [:td (common/fmt-cell (:fill/order-id fill))]
            [:td (common/fmt-cell (:fill/asset fill))]
            (common/side-cell (:fill/side fill))
            [:td.num (common/fmt-cell (:fill/qty fill))]
            [:td.num (when-let [p (:fill/price fill)] (str p))]
            [:td (common/fmt-cell (:fill/id fill))]]))]]]))

(defn- enrich-fill [fill]
  (common/enrich-account-fields fill
                                :fill/account-db
                                :fill/account-name
                                :fill/trader))

(defn query-fills-by-account-pred [conn account-id-pred]
  (->> (q '[:find [(pull ?e [* {:fill/account-db [:account/name :account/trader]}]) ...]
             :in $ ?account-id-pred
             :where
             [?e :fill/account-id ?account-id]
             [(?account-id-pred ?account-id)]
             [?e :fill/id _]]
          @conn account-id-pred)
       (mapv enrich-fill)))

(defn query-all-fills [conn]
  (query-fills-by-account-pred conn (constantly true)))

(defn query-fills [conn {:keys [trader]}]
  (query-fills-by-account-pred conn (accounts-view/account-id-pred conn trader)))
