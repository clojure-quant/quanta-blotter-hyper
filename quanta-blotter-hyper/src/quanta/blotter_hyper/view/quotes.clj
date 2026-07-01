(ns quanta.blotter-hyper.view.quotes
  (:require
   [quanta.blotter-hyper.view.common :as common]))

(defn- price-flash-class
  [flash]
  (case flash
    :up "price-flash-up"
    :down "price-flash-down"
    nil))

(defn- price-cell-class [flash]
  (if-let [fc (price-flash-class flash)]
    (str "price " fc)
    "price"))

(defn- as-num [v]
  (when (some? v) (bigdec v)))

(defn- flash-dir [old-v new-v]
  (let [old (as-num old-v)
        n (as-num new-v)]
    (when (and old n (not= old n))
      (if (pos? (- n old)) :up :down))))

(defn enrich-quotes-with-flash
  "Compare `quotes` to `prev` bid/ask per asset; return enriched map and next prev."
  [quotes prev]
  (let [enriched (into {}
                       (map (fn [[asset q]]
                              (let [{:keys [bid ask]} q
                                    p (get prev asset {})
                                    bid-flash (flash-dir (:bid p) bid)
                                    ask-flash (flash-dir (:ask p) ask)]
                                [asset (assoc q
                                              :bid-flash bid-flash
                                              :ask-flash ask-flash)]))
                            quotes))
        next-prev (into {} (map (fn [[asset {:keys [bid ask]}]]
                                  [asset {:bid bid :ask ask}])
                                quotes))]
    [enriched next-prev]))

(defn- quote-rows
  [quotes]
  (->> (vals quotes)
       (sort-by :asset)))

(defn quotes-table
  [quotes]
  (let [rows (quote-rows quotes)]
    [:div.quotes-table-wrap
     [:table.quotes-table
      [:thead
       [:tr
        [:th "account"]
        [:th "asset"]
        [:th "bid"]
        [:th "ask"]
        [:th "ts"]]]
      [:tbody
       (if (empty? rows)
         [:tr [:td {:colspan 5} "No quotes yet"]]
         (for [{:keys [account asset bid ask ts bid-flash ask-flash]} rows]
           [:tr {:key (str account "-" asset)}
            [:td (common/fmt-cell account)]
            [:td (common/fmt-cell asset)]
            [:td {:class (price-cell-class bid-flash)} (common/fmt-cell bid)]
            [:td {:class (price-cell-class ask-flash)} (common/fmt-cell ask)]
            [:td.time (common/fmt-instant-utc ts)]]))]]]))
