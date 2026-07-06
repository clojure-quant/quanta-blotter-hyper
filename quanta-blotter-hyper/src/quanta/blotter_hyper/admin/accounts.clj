(ns quanta.blotter-hyper.admin.accounts
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter-hyper.view.accounts :as accounts-view]))

(defn- process-query [db-conn _query]
  (accounts-view/query-accounts db-conn {}))

(defn- process-query-f [db-conn query-f data-a]
  (m/ap
   (let [_query (m/?> query-f)
         rows (m/? (m/via m/blk (process-query db-conn _query)))]
     (reset! data-a {:rows rows})
     nil)))

(defn- start-query-processor [db-conn query-f data-a]
  (let [f (process-query-f db-conn query-f data-a)
        t (m/reduce (fn [_r _v] nil) nil f)]
    (t #(println "admin accounts query processor done" %)
       #(println "admin accounts query processor error" %))))

(defn accounts-page
  [{:keys [hyper/env] :as _req}]
  (h/view
   {:mount (fn []
             (let [db (:db env)
                   _ (assert db ":db needs to be in :ctx")
                   data-a (atom nil)
                   query-a (atom {})
                   editing-a (atom nil)
                   edit-value-a (atom "")
                   settings-dialog-a (atom nil)
                   settings-text-a (atom "")
                   settings-error-a (atom nil)
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :query-a query-a
                         :editing-a editing-a
                         :edit-value-a edit-value-a
                         :settings-dialog-a settings-dialog-a
                         :settings-text-a settings-text-a
                         :settings-error-a settings-error-a
                         :db db
                         :dispose! (start-query-processor db query-f data-a)}]
               (h/watch! data-a)
               (h/watch! editing-a)
               (h/watch! edit-value-a)
               (h/watch! settings-dialog-a)
               (h/watch! settings-text-a)
               (h/watch! settings-error-a)
               this))
    :render (fn [{:keys [data-a query-a editing-a edit-value-a
                         settings-dialog-a settings-text-a settings-error-a db]} _req]
              [:motion.div.accounts-page
               ((:admin/nav env))
               [:header.accounts-header
                [:h1 "Accounts"]]
               (if-let [data @data-a]
                 (if (:rows data)
                   (accounts-view/accounts-table (:rows data)
                                                 {:show-trader? true
                                                  :editable? true
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
