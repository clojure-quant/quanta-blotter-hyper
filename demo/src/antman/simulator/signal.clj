(ns antman.simulator.signal
  (:require
   [camel-snake-kebab.core :as csk]
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(def resource-path "antman/signal.json")

(def ^:private optional-nil-fields
  #{:net-r :closed-at :close-reason :broker-position-id})

(defn load-signal
  "Load the default trade signal from classpath JSON."
  []
  (-> (io/resource resource-path)
      slurp
      (json/parse-string (fn [k] (keyword (csk/->kebab-case-string k))))))

(defn field-default
  "Default value for a form field; nil becomes empty string."
  [trade path]
  (let [v (get-in trade path)]
    (if (and (contains? optional-nil-fields (if (vector? path) (last path) path))
             (nil? v))
      ""
      (or v ""))))

(defn blank->nil
  [v]
  (if (and (string? v) (str/blank? v)) nil v))

(defn normalize-field
  [path v]
  (let [k (if (vector? path) (last path) path)]
    (cond
      (= path [:risk-decision :violations])
      (if (string? v)
        (json/parse-string v)
        v)

      (contains? optional-nil-fields k)
      (blank->nil v)

      :else v)))

(defn assemble-trade
  "Build a trade map from dereferenced signal values."
  [fields]
  (reduce-kv
   (fn [m path v]
     (assoc-in m path (normalize-field path v)))
   {}
   fields))
