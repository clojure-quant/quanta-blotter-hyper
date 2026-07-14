(ns quanta.blotter-hyper.view.common
  (:require
   [tick.core :as t]))

(def ^:private utc-fmt (t/formatter "yyyy-MM-dd HH:mm:ss.SSS"))

(defn fmt-instant-utc [v]
  (when v
    (t/format utc-fmt (t/in (t/instant v) t/UTC))))

(defn fmt-cell [v]
  (cond
    (nil? v) "—"
    (keyword? v) (name v)
    :else (str v)))

(defn pos-num? [n]
  (and (some? n) (pos? (bigdec n))))

(defn fmt-pos-num [n]
  (when (pos-num? n) (str n)))

(defn side-cell [side]
  (case side
    :buy  [:td.side.profit "B"]
    :long [:td.side.profit "L"]
    :sell [:td.side.loss "S"]
    :short [:td.side.loss "S"]
    [:td.side (fmt-cell side)]))

(defn enrich-account-fields
  "Join account fields pulled via *-db ref into flat entity keys."
  [entity account-db-key account-name-key trader-key]
  (if-let [account (get entity account-db-key)]
    (-> entity
        (assoc account-name-key (:account/name account)
               trader-key (:account/trader account))
        (dissoc account-db-key))
    entity))

(defn substring-pred
  "Datahike predicate: when `s` is non-empty, match strings that include it;
  otherwise match any value."
  [s]
  (if (seq s)
    (fn [v]
      (and (string? v) (clojure.string/includes? v s)))
    (constantly true)))

(defn parse-account-id
  "Parse account input to a long when it is a valid integer string; else nil."
  [s]
  (some-> s str clojure.string/trim not-empty parse-long))

(defn account-filter-pred
  "AND trader account pred with optional exact account-id."
  [trader-pred account-id]
  (if account-id
    (fn [id] (and (= id account-id) (trader-pred id)))
    trader-pred))
