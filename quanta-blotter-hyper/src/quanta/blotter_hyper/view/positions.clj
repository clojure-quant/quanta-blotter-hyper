(ns quanta.blotter-hyper.view.positions
  (:require
   [datahike.api :refer [q]]
   [quanta.blotter-hyper.view.common :as common]))

(defn- fmt-open? [open?]
  (when open? "Y"))

(defn positions-table
  [positions]
  (let [positions (sort-by :position/date-open #(compare %2 %1) positions)]
    [:div.orders-table-wrap
     [:table.orders-table
      [:thead
       [:tr
        [:th.time "open"]
        [:th "acct"]
        [:th "trader"]
        [:th "acct name"]
        [:th "asset"]
        [:th.side-col "D"]
        [:th "open?"]
        [:th.num "qty-open"]
        [:th.num "qty"]
        [:th.num "avg-in"]
        [:th.num "avg-out"]
        [:th.num "pl"]
        [:th.time "close"]]]
      [:tbody
       (if (empty? positions)
         [:tr [:td {:colspan 13} "No positions"]]
         (for [pos positions]
           [:tr {:key (str (:position/account pos) "-"
                            (:position/asset pos) "-"
                            (name (or (:position/side pos) :none)))}
            [:td.time (common/fmt-instant-utc (:position/date-open pos))]
            [:td (common/fmt-cell (:position/account pos))]
            [:td (common/fmt-cell (:position/trader pos))]
            [:td (common/fmt-cell (:position/account-name pos))]
            [:td (common/fmt-cell (:position/asset pos))]
            (common/side-cell (:position/side pos))
            [:td (fmt-open? (:position/open pos))]
            [:td.num (common/fmt-cell (:position/qty-open pos))]
            [:td.num (common/fmt-cell (:position/qty pos))]
            [:td.num (when-let [v (:position/average-entry-price pos)] (str v))]
            [:td.num (when-let [v (:position/avg-exit-price pos)] (str v))]
            [:td.num (when-let [v (:position/realized-pl pos)] (str v))]
            [:td.time (common/fmt-instant-utc (:position/date-close pos))]]))]]]))

(defn- enrich-position [position]
  (common/enrich-account-fields position
                                :position/account-db
                                :position/account-name
                                :position/trader))

(defn query-all-positions [conn]
  (->> (q '[:find [(pull ?e [* {:position/account-db [:account/name :account/trader]}]) ...]
             :where [?e :position/account _]]
          @conn)
       (mapv enrich-position)))
