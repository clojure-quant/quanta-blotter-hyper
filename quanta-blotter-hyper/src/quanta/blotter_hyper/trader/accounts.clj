(ns quanta.blotter-hyper.trader.accounts
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter-hyper.view.accounts :as accounts-view]
   [quanta.blotter.oms.db :as db]))

(def ^:private query-options {})

(def ^:private api-options
  [[:fix-trade "fix-trade"]
   [:bybit-trade "bybit-trade"]
   [:paper "paper"]])

(defn process-query [db-conn query]
  (accounts-view/query-accounts db-conn query))

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

(defn- accounts-header [api-a query-a db-conn trader]
  [:header.accounts-header
   [:h1 "Accounts"]
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
  [{:keys [hyper/env] :as _req}]
  (h/view
   {:mount (fn []
             (let [db (:db env)
                   _ (assert db ":db needs to be in :ctx")
                   identity @(h/session-cursor :identity)
                   trader (name (:user identity))
                   data-a (atom nil)
                   api-a (atom :paper)
                   query-a (atom (assoc query-options :trader trader))
                   editing-a (atom nil)
                   edit-value-a (atom "")
                   settings-dialog-a (atom nil)
                   settings-text-a (atom "")
                   settings-error-a (atom nil)
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :api-a api-a
                         :query-a query-a
                         :editing-a editing-a
                         :edit-value-a edit-value-a
                         :settings-dialog-a settings-dialog-a
                         :settings-text-a settings-text-a
                         :settings-error-a settings-error-a
                         :trader trader
                         :db db
                         :dispose! (start-query-processor db query-f data-a)}]
               (h/watch! data-a)
               (h/watch! editing-a)
               (h/watch! edit-value-a)
               (h/watch! settings-dialog-a)
               (h/watch! settings-text-a)
               (h/watch! settings-error-a)
               this))
    :render (fn [{:keys [data-a api-a query-a editing-a edit-value-a
                         settings-dialog-a settings-text-a settings-error-a
                         trader db]} _req]
              [:motion.div.accounts-page
               ((:trader/nav env))
               (accounts-header api-a query-a db trader)
               (if-let [data @data-a]
                 (if (:rows data)
                   (accounts-view/accounts-table (:rows data)
                                                 {:editable? true
                                                  :editing-a editing-a
                                                  :edit-value-a edit-value-a
                                                  :settings-dialog-a settings-dialog-a
                                                  :settings-text-a settings-text-a
                                                  :settings-error-a settings-error-a
                                                  :db db
                                                  :query-a query-a})
                   [:p "Loading…"])
                 [:p "Loading…"])])
    :unmount (fn [{:keys [dispose!]}]
               (dispose!))}))
