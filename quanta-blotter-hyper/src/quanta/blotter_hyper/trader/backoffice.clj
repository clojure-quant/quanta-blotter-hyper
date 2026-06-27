(ns quanta.blotter-hyper.trader.backoffice
  (:require
   [tick.core :as t]
   [missionary.core :as m]
   [datahike.api :as d]
   [hyper.core :as h]))

(defn query-all-orders [conn]
  (d/q '[:find [(pull ?e [*]) ...]
         :where [?e :order/id _]]
       @conn))

(defn query-account-orders [conn account-id]
  (d/q '[:find [(pull ?e [*]) ...]
         :in $ ?account-id
         :where
         [?e :order/account-id ?account-id]
         [?e :order/id _]]
       @conn account-id))

(defn process-query [db-conn {:keys [account-id] :as query}]
  (cond 
     account-id (query-account-orders db-conn account-id)
     :else (query-all-orders db-conn)))

(defn process-query-f [db-conn query-f data-a]
  (m/ap 
   (let [query (m/?> query-f)
         data (m/? (m/via m/blk (process-query db-conn query)))]
     (reset! data-a data)
     )))

(defn start-query-processor [db-conn query-f data-a]
  (let [f (process-query-f db-conn query-f data-a)
        t (m/reduce (fn [_r _v]  nil) nil f)]
    (t #(println "query processor done" %) #(println "query-processor error" %))))
 
  

(defn backoffice-page
  [{:keys [ctx] :as _req}]
  (println "BACKOFFICE-PAGE")
  (h/view
   {:mount (fn []
             (println "MOUNTING BACKOFFICE-PAGE")
             (let [db (:db ctx)
                   _ (assert db ":db needs to be in :ctx")
                   _ (println db)     
                   data-a (atom nil)
                   query-a (atom {})
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :dispose! (start-query-processor db query-f data-a)
                         }]
               (h/watch! data-a)
               this))
    :render (fn [{:keys [data-a]} req]
              [:motion.div
               ;(ui/nav)
               [:h1 "Backoffice"]
               [:pre (pr-str @data-a)]
               ;(ui/quotelist-table @quotelist)
               ])
    :unmount (fn [{:keys [dispose!]}]
               (println "UNMOUNTING Backoffice-PAGE")
               (dispose!))}))

