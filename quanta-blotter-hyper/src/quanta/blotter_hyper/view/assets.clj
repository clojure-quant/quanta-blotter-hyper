(ns quanta.blotter-hyper.view.assets
  (:require
   [datahike.api :refer [q]]
   [quanta.blotter-hyper.view.common :as common]))

(def ^:private asset-cols
  [[:asset/symbol "symbol"]
   [:asset/name "name"]
   [:asset/exchange "exchange"]
   [:asset/margin "margin"]
   [:asset/default-quote-account "default quote account"]])

(defn assets-table [assets]
  (let [assets (sort-by :asset/symbol assets)
        col-count (count asset-cols)]
    [:div.orders-table-wrap
     [:table.orders-table
      [:thead
       [:tr
        (for [[_k label] asset-cols]
          [:th label])]]
      [:tbody
       (if (empty? assets)
         [:tr [:td {:colspan col-count} "No assets"]]
         (for [asset assets]
           [:tr {:key (:asset/symbol asset)}
            (for [[k _label] asset-cols]
              [:td (common/fmt-cell (get asset k))])]))]]]))

(defn query-all-assets [conn]
  (q '[:find [(pull ?e [:asset/symbol
                        :asset/name
                        :asset/exchange
                        :asset/margin
                        :asset/default-quote-account]) ...]
       :where [?e :asset/symbol _]]
     @conn))

(defn query-assets [conn _query]
  (query-all-assets conn))
