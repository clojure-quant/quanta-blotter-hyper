(ns quanta.blotter-hyper.view.assets
  (:require
   [clojure.string :as str]
   [datahike.api :as d]
   [hyper.core :as h]
   [hyper.effects :as effects]
   [quanta.blotter-hyper.view.common :as common]))

(def ^:private editable-keys
  #{:asset/margin :asset/price-point :asset/quantity-step})

(def ^:private asset-cols
  [[:asset/symbol "symbol"]
   [:asset/name "name"]
   [:asset/exchange "exchange"]
   [:asset/currency "currency"]
   [:asset/margin "margin"]
   [:asset/price-point "price point"]
   [:asset/quantity-step "quantity step"]
   [:asset/default-quote-account "default quote account"]])

(defn- parse-positive-int [s]
  (when-let [s (not-empty (str/trim (str s)))]
    (when (re-matches #"[0-9]+" s)
      (let [n (parse-long s)]
        (when (pos? n) n)))))

(defn- parse-positive-bigdec [s]
  (when-let [s (not-empty (str/trim (str s)))]
    (try
      (let [n (bigdec s)]
        (when (pos? n) n))
      (catch Exception _ nil))))

(defn- field-text [asset field]
  (let [v (get asset field)]
    (cond
      (nil? v) ""
      (= field :asset/margin) (str (long v))
      :else (str v))))

(defn- edit-text [asset edits-a field]
  (or (get-in @edits-a [(:asset/symbol asset) field])
      (field-text asset field)))

(defn- set-edit-text! [edits-a asset field text]
  (let [symbol (:asset/symbol asset)
        orig (field-text asset field)]
    (swap! edits-a
           (fn [m]
             (if (= text orig)
               (let [sym-edits (dissoc (get m symbol) field)]
                 (if (empty? sym-edits)
                   (dissoc m symbol)
                   (assoc m symbol sym-edits)))
               (assoc-in m [symbol field] text))))))

(defn asset-dirty?
  "True when the given asset has unsaved field edits."
  [edits-a symbol]
  (boolean (seq (get @edits-a symbol))))

(defn- parse-field-value [field text]
  (case field
    :asset/margin (parse-positive-int text)
    :asset/price-point (parse-positive-bigdec text)
    :asset/quantity-step (parse-positive-bigdec text)
    nil))

(defn- asset-by-symbol [conn symbol]
  (d/pull @conn [:db/id] [:asset/symbol symbol]))

(defn- build-save-tx [conn symbol field-texts]
  (when-let [entity (asset-by-symbol conn symbol)]
    (let [parsed (into {}
                       (keep (fn [[field text]]
                               (when-let [v (parse-field-value field text)]
                                 [field v]))
                             field-texts))]
      (when (= (count parsed) (count field-texts))
        (assoc parsed :db/id (:db/id entity))))))

(defn save-asset-edits!
  "Save the pending edits for a single asset identified by symbol."
  [conn edits-a query-a symbol]
  (when-let [field-texts (seq (get @edits-a symbol))]
    (when-let [tx (build-save-tx conn symbol field-texts)]
      (d/transact conn [tx])
      (swap! edits-a dissoc symbol)
      (when query-a (swap! query-a update :n (fnil inc 0)))
      (effects/execute-script! "quantaBeep()"))))

(defn- editable-cell [asset field edits-a]
  [:td.num.asset-editable-cell
   [:input.asset-field-input
    {:type "text"
     :value (edit-text asset edits-a field)
     :data-on:input (h/action (set-edit-text! edits-a asset field $value))}]])

(defn- asset-save-cell [asset {:keys [edits-a db query-a]}]
  (let [symbol (:asset/symbol asset)]
    [:td.asset-save-cell
     (when (asset-dirty? edits-a symbol)
       [:button.assets-save
        {:type "button"
         :data-on:click (h/action (save-asset-edits! db edits-a query-a symbol))}
        "SAVE"])]))

(defn- asset-row [asset {:keys [editable? edits-a] :as opts}]
  [:tr {:key (:asset/symbol asset)}
   (for [[field _label] asset-cols]
     (if (and editable? (contains? editable-keys field))
       (editable-cell asset field edits-a)
       [:td (if (#{:asset/margin :asset/price-point :asset/quantity-step} field)
              {:class "num"}
              {})
        (common/fmt-cell (get asset field))]))
   (when editable?
     (asset-save-cell asset opts))])

(defn assets-table
  ([assets]
   (assets-table assets {}))
  ([assets {:keys [editable? edits-a db query-a]}]
   (let [assets (sort-by (juxt :asset/exchange :asset/symbol) assets)
         col-count (cond-> (count asset-cols) editable? inc)
         row-opts (when editable?
                    {:editable? true :edits-a edits-a :db db :query-a query-a})]
     [:div.orders-table-wrap
      [:table.orders-table
       [:thead
        [:tr
         (for [[_k label] asset-cols]
           [:th (if (#{"margin" "price point" "quantity step"} label)
                  {:class "num"}
                  {})
            label])
         (when editable? [:th ""])]]
       [:tbody
        (if (empty? assets)
          [:tr [:td {:colspan col-count} "No assets"]]
          (for [asset assets]
            (asset-row asset (or row-opts {}))))]]])))

(defn query-all-assets [conn]
  (d/q '[:find [(pull ?e [:db/id
                          :asset/symbol
                          :asset/name
                          :asset/exchange
                          :asset/currency
                          :asset/margin
                          :asset/price-point
                          :asset/quantity-step
                          :asset/default-quote-account]) ...]
         :where [?e :asset/symbol _]]
       @conn))

(defn query-assets [conn _query]
  (query-all-assets conn))
