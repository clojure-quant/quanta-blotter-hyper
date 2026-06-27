(ns antman.sse-heartbeat-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [antman.sse.heartbeat :as heartbeat]))

(use-fixtures :each
  (fn [f]
    (heartbeat/stop!)
    (reset! heartbeat/server-ts* 0)
    (f)
    (heartbeat/stop!)
    (reset! heartbeat/server-ts* 0)))

(deftest heartbeat-updates-timestamp-test
  (testing "start! publishes millis timestamps about once per second"
    (heartbeat/start!)
    (Thread/sleep 1200)
    (is (pos? @heartbeat/server-ts*))
    (let [t0 @heartbeat/server-ts*]
      (Thread/sleep 1200)
      (is (> @heartbeat/server-ts* t0)))))
