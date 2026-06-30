(ns quanta.blotter-hyper.db.schema
  (:require
   [quanta.asset.schema :as asset-schema]
   [quanta.blotter.oms.db :as oms-db]))

(def extra-asset-attrs
  [{:db/ident :asset/margin
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}
   {:db/ident :asset/default-quote-account
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one}])

(def schema
  (->> (concat oms-db/schema asset-schema/schema extra-asset-attrs)
       vec))
