(ns quanta.blotter-hyper.trader.backoffice
  (:require
   [tick.core :as t]
   [missionary.core :as m]
   [datahike.api :as d]
   [hyper.core :as h]))

(def ^:private utc-fmt (t/formatter "yyyy-MM-dd HH:mm:ss.SSS"))

(defn- fmt-instant-utc [v]
  (when v
    (t/format utc-fmt (t/in (t/instant v) t/UTC))))

(defn- fmt-cell [v]
  (cond
    (nil? v) "—"
    (keyword? v) (name v)
    :else (str v)))

(defn- pos-num? [n]
  (and (some? n) (pos? (bigdec n))))

(defn- fmt-pos-num [n]
  (when (pos-num? n) (str n)))

(defn- side-cell [side]
  (case side
    :buy [:td.side.profit "B"]
    :sell [:td.side.loss "S"]
    [:td.side (fmt-cell side)]))

(defn- order-type-cell [order-type]
  (case order-type
    :stop [:td.ot "STP"]
    :limit [:td.ot "LMT"]
    :market [:td.ot "MKT"]
    [:td.ot (fmt-cell order-type)]))

(defn- wkg-cell [qty-working]
  [:td {:class (str "wkg" (when (pos-num? qty-working) " wkg-active"))}
   (fmt-cell qty-working)])

(defn- status-cell [status]
  [:td {:class (str "status"
                    (case status
                      :filled " status-filled"
                      :rejected " status-muted"
                      :cancelled " status-muted"
                      :working " status-working"
                      ""))}
   (fmt-cell status)])

(defn orders-table
  [orders]
  (let [orders (sort-by :order/date #(compare %2 %1) orders)]
    [:div.orders-table-wrap
     [:table.orders-table
     [:thead
      [:tr
       [:th.time "time"]
       [:th "acct"]
       [:th "camp"]
       [:th "lbl"]
       [:th "id"]
       [:th "asset"]
       [:th.side-col "D"]
       [:th.num "qty"]
       [:th "OT"]
       [:th "lmt"]
       [:th "status"]
       [:th "wkg"]
       [:th.num "fill"]
       [:th.num "avg"]
       [:th "Message"]]]
     [:tbody
      (if (empty? orders)
        [:tr [:td {:colspan 15} "No orders"]]
        (for [order orders]
          [:tr {:key (:order/id order)}
           [:td.time (fmt-instant-utc (:order/date order))]
           [:td (fmt-cell (:order/account-id order))]
           [:td (fmt-cell (:order/campaign order))]
           [:td (fmt-cell (:order/label order))]
           [:td (fmt-cell (:order/id order))]
           [:td (fmt-cell (:order/asset order))]
           (side-cell (:order/side order))
           [:td.num (fmt-cell (:order/qty order))]
           (order-type-cell (:order/type order))
           [:td (fmt-cell (:order/limit order))]
           (status-cell (:order/status order))
           (wkg-cell (:order/qty-working order))
           [:td.num (fmt-pos-num (:order/qty-filled order))]
           [:td.num (when-let [avg (:order/avg-price order)] (str avg))]
           [:td (fmt-cell (:order/text order))]]))]]]))

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
               (if-let [orders @data-a]
                 (orders-table orders)
                 [:p "Loading orders…"])
               ])
    :unmount (fn [{:keys [dispose!]}]
               (println "UNMOUNTING Backoffice-PAGE")
               (dispose!))}))

