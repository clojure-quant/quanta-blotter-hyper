(ns quanta.blotter-hyper.trader.send-order
  (:require
   [missionary.core :as m]
   [nano-id.core :refer [nano-id]]
   [hyper.core :as h]
   [quanta.blotter.oms.core :as oms]
   [quanta.blotter.oms.db :as db]
   [quanta.blotter.oms.validation.schema :as schema]))

(defn available-assets
  []
  ["EURUSD"
   "USDJPY"
   "BTCUSDT.LF.BB"])

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
    :order-id (new-order-id)
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

(defn valid-new-order?
  [state]
  (schema/validate-trader-message
   (assoc (state->order-details state) :type :trader/new-order)))

(defn reset-order-id!
  [state-a]
  (swap! state-a assoc :order-id (new-order-id)))

(defn submit!
  "Submit a new order to the order manager and reset `:order-id`."
  [oms state-a]
  (let [state @state-a
        details (state->order-details state)]
    (when (valid-new-order? state)
      (m/? (oms/create-order oms details))
      (reset-order-id! state-a))))

(defn- select-field
  [label value options on-change]
  [:label.send-order-field
   [:span.send-order-label label]
   [:select {:value (str value)
             :data-on:change (h/action (on-change $value))}
    (for [[opt-val opt-label] options]
      [:option {:key (str opt-val)
                :value (str opt-val)
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
  "Reusable send-order panel. Expects `state-a` atom, `accounts` map from
  `trader-accounts`, `assets` from `available-assets`, and `oms`."
  [{:keys [state-a accounts assets oms]}]
  (let [state @state-a
        update-field (fn [k parse]
                       (fn [v]
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
                    (update-field :asset identity))
      (select-field "side" (:side state)
                    [[:buy "buy"] [:sell "sell"]]
                    (update-field :side keyword))
      (select-field "order-type" (:order-type state)
                    [[:limit "limit"] [:market "market"] [:stop "stop"]]
                    (update-field :order-type keyword))
      (when show-limit?
        (num-field "limit" (:limit state)
                   (update-field :limit bigdec)))
      (num-field "qty" (:qty state)
                 (update-field :qty bigdec))
      (text-field "campaign" (:campaign state) identity {:readonly true})
      (text-field "label" (:label state) identity {:readonly true})
      [:button.send-order-submit
       {:type "button"
        :data-on:click (h/action (submit! oms state-a))}
       "Send order"]]]))
