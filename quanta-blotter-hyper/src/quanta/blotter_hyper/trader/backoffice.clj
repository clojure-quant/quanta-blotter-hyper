(ns quanta.blotter-hyper.trader.backoffice
  (:require
   [tick.core :as t]
   [missionary.core :as m]
   [datahike.api :refer [q]]
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

(defn fill-table
  [fills]
  (let [fills (sort-by :fill/date #(compare %2 %1) fills)]
    [:div.orders-table-wrap
     [:table.orders-table
      [:thead
       [:tr
        [:th.time "time"]
        [:th "acct"]
        [:th "camp"]
        [:th "lbl"]
        [:th "order-id"]
        [:th "asset"]
        [:th.side-col "D"]
        [:th.num "qty"]
        [:th.num "price"]
        [:th "id"]]]
      [:tbody
       (if (empty? fills)
         [:tr [:td {:colspan 10} "No trades"]]
         (for [fill fills]
           [:tr {:key (:fill/id fill)}
            [:td.time (fmt-instant-utc (:fill/date fill))]
            [:td (fmt-cell (:fill/account-id fill))]
            [:td (fmt-cell (:fill/campaign fill))]
            [:td (fmt-cell (:fill/label fill))]
            [:td (fmt-cell (:fill/order-id fill))]
            [:td (fmt-cell (:fill/asset fill))]
            (side-cell (:fill/side fill))
            [:td.num (fmt-cell (:fill/qty fill))]
            [:td.num (when-let [p (:fill/price fill)] (str p))]
            [:td (fmt-cell (:fill/id fill))]]))]]]))

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
         [:tr [:td {:colspan 11} "No positions"]]
         (for [pos positions]
           [:tr {:key (str (:position/account pos) "-"
                            (:position/asset pos) "-"
                            (name (or (:position/side pos) :none)))}
            [:td.time (fmt-instant-utc (:position/date-open pos))]
            [:td (fmt-cell (:position/account pos))]
            [:td (fmt-cell (:position/asset pos))]
            (side-cell (:position/side pos))
            [:td (fmt-open? (:position/open pos))]
            [:td.num (fmt-cell (:position/qty-open pos))]
            [:td.num (fmt-cell (:position/qty pos))]
            [:td.num (when-let [v (:position/average-entry-price pos)] (str v))]
            [:td.num (when-let [v (:position/avg-exit-price pos)] (str v))]
            [:td.num (when-let [v (:position/realized-pl pos)] (str v))]
            [:td.time (fmt-instant-utc (:position/date-close pos))]]))]]]))

(defn query-all-orders [conn]
  (q '[:find [(pull ?e [*]) ...]
         :where [?e :order/id _]]
       @conn))

(defn query-account-orders [conn account-id]
  (q '[:find [(pull ?e [*]) ...]
         :in $ ?account-id
         :where
         [?e :order/account-id ?account-id]
         [?e :order/id _]]
       @conn account-id))

(defn query-all-fills [conn]
  (q '[:find [(pull ?e [*]) ...]
         :where [?e :fill/id _]]
       @conn))

(defn query-all-positions [conn]
  (q '[:find [(pull ?e [*]) ...]
         :where [?e :position/account _]]
       @conn))

(defn process-query [db-conn {:keys [table account-id]}]
  (case table
    :orders (cond
              account-id (query-account-orders db-conn account-id)
              :else (query-all-orders db-conn))
    :trades (query-all-fills db-conn)
    :positions (query-all-positions db-conn)
    []))

(defn process-query-f [db-conn query-f data-a]
  (m/ap
   (let [query (m/?> query-f)
         data (m/? (m/via m/blk (process-query db-conn query)))]
     (reset! data-a {:table (:table query) :rows data})
     )))

(defn start-query-processor [db-conn query-f data-a]
  (let [f (process-query-f db-conn query-f data-a)
        t (m/reduce (fn [_r _v]  nil) nil f)]
    (t #(println "query processor done" %) #(println "query-processor error" %))))

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
    :orders (orders-table rows)
    :trades (fill-table rows)
    :positions (positions-table rows)
    [:p "Unknown table"]))

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
                   query-a (atom {:table :orders})
                   query-f (m/watch query-a)
                   this {:data-a data-a
                         :query-a query-a
                         :dispose! (start-query-processor db query-f data-a)
                         }]
               (h/watch! data-a)
               this))
    :render (fn [{:keys [data-a query-a]} _req]
              [:motion.div.backoffice-page
               (backoffice-header data-a query-a)
               (if-let [data @data-a]
                 (if (:rows data)
                   (render-table data)
                   [:p "Loading…"])
                 [:p "Loading…"])])
    :unmount (fn [{:keys [dispose!]}]
               (println "UNMOUNTING Backoffice-PAGE")
               (dispose!))}))

