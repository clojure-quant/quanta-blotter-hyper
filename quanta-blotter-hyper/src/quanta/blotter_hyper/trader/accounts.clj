(ns quanta.blotter-hyper.trader.accounts
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter.oms.db :as db]))

(defn- fmt-cell [v]
  (cond
    (nil? v) "—"
    (boolean? v) (str v)
    (keyword? v) (name v)
    :else (str v)))

(defn- fmt-settings [settings]
  (if (some? settings)
    (pr-str settings)
    "—"))

(defn accounts-table
  [accounts]
  (let [accounts (sort-by :account/id accounts)]
    [:div.orders-table-wrap
     [:table.orders-table
      [:thead
       [:tr
        [:th "account id"]
        [:th "account name"]
        [:th.num "account balance"]
        [:th "enabled"]
        [:th "api type"]
        [:th "settings"]]]
      [:tbody
       (if (empty? accounts)
         [:tr [:td {:colspan 6} "No accounts"]]
         (for [account accounts]
           [:tr {:key (:account/id account)}
            [:td (fmt-cell (:account/id account))]
            [:td (fmt-cell (:account/name account))]
            [:td.num (fmt-cell (:account/balance account))]
            [:td (fmt-cell (:account/enabled account))]
            [:td (fmt-cell (:account/api account))]
            [:td.settings (fmt-settings (:account/settings account))]]))]]]))

(def ^:private query-options {})

(def ^:private api-options
  [[:fix-trade "fix-trade"]
   [:bybit-trade "bybit-trade"]
   [:paper "paper"]])

(defn- query-trader-accounts [db-conn trader]
  (db/trader-account-list db-conn trader))

(defn process-query [db-conn {:keys [trader]}]
  (if trader
    (query-trader-accounts db-conn trader)
    []))

(defn process-query-f [db-conn query-f data-a]
  (m/ap
   (let [query (m/?> query-f)
         rows (m/? (m/via m/blk (process-query db-conn query)))]
     (reset! data-a {:rows rows})
     nil)))

(defn start-query-processor [db-conn query-f data-a]
  (let [f (process-query-f db-conn query-f data-a)
        t (m/reduce (fn [_r _v] nil) nil f)]
    (t #(println "accounts query processor done" %)
       #(println "accounts query processor error" %))))

(defn- accounts-header [trader api-a query-a db-conn]
  [:header.accounts-header
   [:h1 "Accounts"]
   [:span.accounts-trader (str "Trader: " trader)]
   [:select {:data-on:change
             (h/action (reset! api-a (keyword $value)))}
    (for [[kw label] api-options]
      [:option {:value (name kw) :selected (= kw @api-a)} label])]
   [:button.accounts-add {:data-on:click
                           (h/action
                            (db/create-account db-conn
                                               {:account/trader trader
                                                :account/api @api-a})
                            (swap! query-a update :n (fnil inc 0)))}
    "Add account"]])

(defn accounts-page
  [{:keys [ctx] :as _req}]
  (h/view
   {:mount (fn []
             (let [db (:db ctx)
                   _ (assert db ":db needs to be in :ctx")
                   identity @(h/session-cursor :identity)
                   trader (name (:user identity))
                   data-a (atom nil)
                   api-a (atom :paper)
                   query-a (atom (assoc query-options :trader trader))
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :api-a api-a
                         :query-a query-a
                         :trader trader
                         :db db
                         :dispose! (start-query-processor db query-f data-a)}]
               (h/watch! data-a)
               this))
    :render (fn [{:keys [data-a api-a query-a trader db]} _req]
              [:motion.div.accounts-page
               (accounts-header trader api-a query-a db)
               (if-let [data @data-a]
                 (if (:rows data)
                   (accounts-table (:rows data))
                   [:p "Loading…"])
                 [:p "Loading…"])])
    :unmount (fn [{:keys [dispose!]}]
               (dispose!))}))
