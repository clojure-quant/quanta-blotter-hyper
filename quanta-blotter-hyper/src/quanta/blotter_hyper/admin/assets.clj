(ns quanta.blotter-hyper.admin.assets
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter-hyper.view.assets :as assets-view]))

(defn- process-query [db-conn _query]
  (assets-view/query-assets db-conn {}))

(defn- process-query-f [db-conn query-f data-a]
  (m/ap
   (let [_query (m/?> query-f)
         rows (m/? (m/via m/blk (process-query db-conn _query)))]
     (reset! data-a {:rows rows})
     nil)))

(defn- start-query-processor [db-conn query-f data-a]
  (let [f (process-query-f db-conn query-f data-a)
        t (m/reduce (fn [_r _v] nil) nil f)]
    (t #(println "admin assets query processor done" %)
       #(println "admin assets query processor error" %))))

(defn assets-page
  [{:keys [hyper/env] :as _req}]
  (h/view
   {:mount (fn []
             (let [db (:db env)
                   _ (assert db ":db needs to be in :ctx")
                   data-a (atom nil)
                   query-a (atom {})
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :dispose! (start-query-processor db query-f data-a)}]
               (h/watch! data-a)
               this))
    :render (fn [{:keys [data-a]} _req]
              [:motion.div.assets-page
               ((:admin/nav env))
               [:header.assets-header
                [:h1 "Assets"]]
               (if-let [data @data-a]
                 (if (:rows data)
                   (assets-view/assets-table (:rows data))
                   [:p "Loading…"])
                 [:p "Loading…"])])
    :unmount (fn [{:keys [dispose!]}]
               (dispose!))}))
