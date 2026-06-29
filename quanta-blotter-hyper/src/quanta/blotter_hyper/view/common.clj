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
    :buy [:td.side.profit "B"]
    :sell [:td.side.loss "S"]
    [:td.side (fmt-cell side)]))
