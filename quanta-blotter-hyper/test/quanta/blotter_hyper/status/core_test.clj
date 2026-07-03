(ns quanta.blotter-hyper.status.core-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [quanta.blotter-hyper.status.core :as core]))

(use-fixtures :each
  (fn [f]
    (core/stop!)
    (reset! core/server-time-a nil)
    (f)
    (core/stop!)
    (reset! core/server-time-a nil)))

(deftest heartbeat-updates-timestamp-test
  (testing "start! publishes instants about once per second"
    (core/start!)
    (Thread/sleep 1200)
    (is (some? @core/server-time-a))
    (let [t0 @core/server-time-a]
      (Thread/sleep 1200)
      (is (.isBefore t0 @core/server-time-a)))))
