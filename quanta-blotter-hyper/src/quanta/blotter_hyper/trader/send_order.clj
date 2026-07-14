(ns quanta.blotter-hyper.trader.send-order
  (:require
   [missionary.core :as m]
   [nano-id.core :refer [nano-id]]
   [hyper.core :as h]
   [quanta.blotter-hyper.component.decimal :refer [decimal-input]]
   [quanta.blotter.oms.core :as oms]
   [quanta.blotter.oms.db :as db]
   [quanta.blotter.oms.validation.schema :as schema]
   [quanta.quote.core :refer [quote-snapshot]]))

(def quote-snapshot-timeout-ms 1000)

(defn available-assets
  []
  ["EURUSD"
   "USDJPY"
   "BTCUSDT.LF.BB"
   "__TEST"
   "__TEST2"])

(defn trader-accounts
  "Returns a map of enabled account id to account name for `trader`, e.g.
  `{1 \"fpaper-2fills\" 1000 \"pepperstone demo1\"}`."
  [db-conn trader]
  (->> (db/trader-account-list db-conn trader)
       (filter :account/enabled)
       (sort-by :account/id)
       (into {} (map (juxt :account/id :account/name)))))

(defn new-order-id
  []
  (nano-id 6))

(defn default-state
  ([]
   (default-state nil))
  ([account-id]
   {:account account-id
    :cancel-account account-id
    :order-id (new-order-id)
    :cancel-order-id ""
    :asset "EURUSD"
    :side :buy
    :order-type :limit
    :limit 1.1035M
    :qty 10000M
    :campaign "manual order"
    :label :manual}))

(defn state->order-details
  "Maps send-order form state to `create-order` arguments."
  [state]
  (cond-> {:account/id (:account state)
           :order-id (:order-id state)
           :asset (:asset state)
           :side (:side state)
           :order-type (:order-type state)
           :qty (:qty state)
           :campaign (:campaign state)
           :label (:label state)}
    (#{:limit :stop} (:order-type state))
    (assoc :limit (:limit state))))

(defn state->trader-message
  [state]
  (assoc (state->order-details state) :type :trader/new-order))

(defn validation-error
  [state]
  (schema/human-error-trader-message (state->trader-message state)))

(defn valid-new-order?
  [state]
  (schema/validate-trader-message (state->trader-message state)))

(defn reset-order-id!
  [state-a]
  (swap! state-a assoc :order-id (new-order-id)))

(defn on-asset-change!
  "Update `:asset` and set `:limit` to the quote bid when a snapshot arrives."
  [quote-manager state-a error-a asset]
  (reset! error-a nil)
  (swap! state-a assoc :asset asset)
  (when-let [quote (m/? (quote-snapshot quote-manager quote-snapshot-timeout-ms asset))]
    (swap! state-a assoc :limit (bigdec (:bid quote)))))

(defn submit!
  "Submit a new order to the order manager and reset `:order-id`."
  [oms state-a error-a]
  (let [state @state-a
        details (state->order-details state)]
    (if (valid-new-order? state)
      (do
        (reset! error-a nil)
        (m/? (oms/create-order oms details))
        (reset-order-id! state-a))
      (reset! error-a (validation-error state)))))

(defn state->cancel-details
  "Maps cancel-order form state to `cancel-order` arguments."
  [state]
  {:account/id (:cancel-account state)
   :order-id (:cancel-order-id state)
   :asset (:asset state)})

(defn state->cancel-message
  [state]
  (assoc (state->cancel-details state) :type :trader/cancel-order))

(defn cancel-validation-error
  [state]
  (if (seq (:cancel-order-id state))
    (schema/human-error-trader-message (state->cancel-message state))
    "order-id required"))

(defn valid-cancel-order?
  [state]
  (boolean
   (and (seq (:cancel-order-id state))
        (schema/validate-trader-message (state->cancel-message state)))))

(defn clear-cancel-order-id!
  [state-a]
  (swap! state-a assoc :cancel-order-id ""))

(defn order->cancel-state
  "Maps a working order to cancel form state."
  [order]
  {:cancel-account (:order/account-id order)
   :cancel-order-id (str (:order/id order))
   :asset (:order/asset order)})

(defn- do-cancel!
  [oms error-a state on-success]
  (let [details (state->cancel-details state)]
    (if (valid-cancel-order? state)
      (do
        (reset! error-a nil)
        (m/? (oms/cancel-order oms details))
        (when on-success (on-success)))
      (reset! error-a (cancel-validation-error state)))))

(defn cancel!
  "Cancel an order via the OMS and clear the cancel order-id field."
  [oms state-a error-a]
  (do-cancel! oms error-a @state-a #(clear-cancel-order-id! state-a)))

(defn cancel-by-order!
  "Cancel a working order via the OMS using its account-id and order-id."
  [oms order error-a]
  (do-cancel! oms error-a (order->cancel-state order) nil))

(defn- keyword-from-select
  "Parse select value to a simple keyword. Avoids `(keyword \":buy\")` -> `::buy`."
  [v]
  (keyword (if (keyword? v)
             (name v)
             (let [s (str v)]
               (if (and (seq s) (= \: (first s)))
                 (subs s 1)
                 s)))))

(defn- select-value-str [v]
  (if (keyword? v) (name v) (str v)))

(defn- select-field
  [label value options on-change]
  [:label.send-order-field
   [:span.send-order-label label]
   [:select {:value (select-value-str value)
             :data-on:change (h/action (on-change $value))}
    (for [[opt-val opt-label] options]
      [:option {:key (select-value-str opt-val)
                :value (select-value-str opt-val)
                :selected (= opt-val value)}
       opt-label])]])

(defn- text-field
  ([label value on-change]
   (text-field label value on-change {}))
  ([label value on-change {:keys [type readonly]}]
   [:label.send-order-field
    [:span.send-order-label label]
    [:input (cond-> {:type (or type "text")
                     :value (str value)
                     :data-on:input (h/action (on-change $value))}
              readonly (assoc :readonly true))]]))

(defn- num-field
  [label value on-change]
  [:label.send-order-field
   [:span.send-order-label label]
   [:input {:type "number"
            :step "any"
            :value (str value)
            :data-on:input (h/action (on-change $value))}]])

(defn- account-options [accounts]
  (for [[id name] (sort-by key accounts)]
    [id (str id " " name)]))

(defn panel
  "Reusable send-order panel. Expects `state-a` atom, `error-a` atom, `accounts`
  map from `trader-accounts`, `assets` from `available-assets`, `oms`, and
  `quote-manager` for asset-change quote snapshots."
  [{:keys [state-a error-a accounts assets oms quote-manager]}]
  (let [state @state-a
        order-data (state->order-details state)
        validation-error @error-a
        update-field (fn [k parse]
                       (fn [v]
                         (reset! error-a nil)
                         (swap! state-a assoc k (parse v))))
        show-limit? (#{:limit :stop} (:order-type state))]
    [:section.send-order-panel
     [:h2 "Send order"]
     [:div.send-order-form
      (select-field "account" (:account state)
                    (account-options accounts)
                    (update-field :account #(Long/parseLong ^String %)))
      (text-field "order-id" (:order-id state) identity {:readonly true})
      (select-field "asset" (:asset state)
                    (map vector assets assets)
                    #(on-asset-change! quote-manager state-a error-a %))
      (select-field "side" (:side state)
                    [[:buy "buy"] [:sell "sell"]]
                    (update-field :side keyword-from-select))
      (select-field "order-type" (:order-type state)
                    [[:limit "limit"] [:market "market"] [:stop "stop"]]
                    (update-field :order-type keyword-from-select))
      [:div#send-order-limit-slot
       (when show-limit?
         [:label.send-order-field
          [:span.send-order-label "limit"]
          (decimal-input
           {:value (str (:limit state))
            :class "small-decimal-editor"
            :data-on:change
            (h/action
             (reset! error-a nil)
             (swap! state-a assoc :limit (bigdec (:value $detail))))})])]
      (num-field "qty" (:qty state)
                 (update-field :qty bigdec))
      (text-field "campaign" (:campaign state)
                  (update-field :campaign identity))
      (text-field "label" (select-value-str (:label state))
                  (update-field :label keyword-from-select))
      [:button.send-order-submit
       {:type "button"
        :data-on:click (h/action (submit! oms state-a error-a))}
       "Send order"]]
     [:div.send-order-form
      (select-field "account" (:cancel-account state)
                    (account-options accounts)
                    (update-field :cancel-account #(Long/parseLong ^String %)))
      (text-field "order-id" (:cancel-order-id state)
                  (update-field :cancel-order-id identity))
      [:button.send-order-cancel
       {:type "button"
        :data-on:click (h/action (cancel! oms state-a error-a))}
       "Cancel order"]]
     (when validation-error
       [:div.send-order-error
        [:p "cannot send order, schema validation error"]
        [:pre (pr-str validation-error)]])
     [:pre.send-order-preview (pr-str order-data)]]))
