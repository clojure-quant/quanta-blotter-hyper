(ns quanta.blotter-hyper.view.accounts
  (:require
   [datahike.api :refer [q]]
   [quanta.blotter-hyper.view.common :as common]
   [quanta.blotter.oms.db :as db]))

(defn- fmt-settings [settings]
  (if (some? settings)
    (pr-str settings)
    "—"))

(defn accounts-table
  ([accounts]
   (accounts-table accounts {}))
  ([accounts {:keys [show-trader?] :or {show-trader? false}}]
   (let [sort-key (if show-trader?
                    (juxt :account/trader :account/id)
                    :account/id)
         accounts (sort-by sort-key accounts)
         cols (if show-trader? 7 6)]
     [:div.orders-table-wrap
      [:table.orders-table
       [:thead
        [:tr
         (when show-trader? [:th "trader"])
         [:th "account id"]
         [:th "account name"]
         [:th.num "account balance"]
         [:th "enabled"]
         [:th "api type"]
         [:th "settings"]]]
       [:tbody
        (if (empty? accounts)
          [:tr [:td {:colspan cols} "No accounts"]]
          (for [account accounts]
            [:tr {:key (:account/id account)}
             (when show-trader?
               [:td (common/fmt-cell (:account/trader account))])
             [:td (common/fmt-cell (:account/id account))]
             [:td (common/fmt-cell (:account/name account))]
             [:td.num (common/fmt-cell (:account/balance account))]
             [:td (common/fmt-cell (:account/enabled account))]
             [:td (common/fmt-cell (:account/api account))]
             [:td.settings (fmt-settings (:account/settings account))]]))]]])))

(defn query-all-accounts [conn]
  (q '[:find [(pull ?e [*]) ...]
         :where [?e :account/id _]]
       @conn))

(defn query-trader-accounts [conn trader]
  (db/trader-account-list conn trader))

(defn query-accounts [conn {:keys [trader]}]
  (if trader
    (query-trader-accounts conn trader)
    (query-all-accounts conn)))
