(ns quanta.blotter-hyper.trader.backoffice
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.blotter-hyper.nav :as nav]
   [quanta.blotter-hyper.view.orders :as orders-view]
   [quanta.blotter-hyper.view.trades :as trades-view]
   [quanta.blotter-hyper.view.positions :as positions-view]))

(defn- process-query [db-conn {:keys [table trader]}]
  (case table
    :orders (orders-view/query-orders db-conn {:trader trader})
    :trades (trades-view/query-fills db-conn {:trader trader})
    :positions (positions-view/query-positions db-conn {:trader trader})
    []))

(defn- process-query-f [db-conn query-f data-a]
  (m/ap
   (let [query (m/?> query-f)
         data (m/? (m/via m/blk (process-query db-conn query)))]
     (reset! data-a {:table (:table query) :rows data})
     )))

(defn- start-query-processor [db-conn query-f data-a]
  (let [f (process-query-f db-conn query-f data-a)
        t (m/reduce (fn [_r _v] nil) nil f)]
    (t #(println "query processor done" %)
       #(println "query-processor error" %))))

(def ^:private table-options
  [[:orders "orders"]
   [:trades "trades"]
   [:positions "positions"]])

(defn- backoffice-header [data-a query-a]
  (let [current-table (or (:table @data-a) :orders)]
    [:header.backoffice-header
     [:h1 "Backoffice"]
     [:select {:data-on:change
               (h/action
                (let [t (keyword $value)]
                  (swap! query-a assoc :table t)
                  (swap! data-a assoc :table t)))}
      (for [[kw label] table-options]
        [:option {:value (name kw) :selected (= kw current-table)} label])]]))

(defn- render-table [{:keys [table rows]}]
  (case table
    :orders (orders-view/orders-table rows)
    :trades (trades-view/fill-table rows)
    :positions (positions-view/positions-table rows)
    [:p "Unknown table"]))

(defn backoffice-page*
  [nav-fn {:keys [ctx] :as _req} query-options]
  (h/view
   {:mount (fn []
             (let [db (:db ctx)
                   _ (assert db ":db needs to be in :ctx")
                   data-a (atom nil)
                   query-a (atom (merge {:table :orders} query-options))
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :query-a query-a
                         :dispose! (start-query-processor db query-f data-a)}]
               (h/watch! data-a)
               this))
    :render (fn [{:keys [data-a query-a]} _req]
              [:motion.div.backoffice-page
               (nav-fn)
               (backoffice-header data-a query-a)
               (if-let [data @data-a]
                 (if (:rows data)
                   (render-table data)
                   [:p "Loading…"])
                 [:p "Loading…"])])
    :unmount (fn [{:keys [dispose!]}]
               (dispose!))}))

(defn backoffice-page
  ([req]
   (let [identity @(h/session-cursor :identity)
         trader (name (:user identity))]
     (backoffice-page* nav/trader-nav req {:trader trader})))
  ([nav-fn req query-options]
   (backoffice-page* nav-fn req query-options)))
