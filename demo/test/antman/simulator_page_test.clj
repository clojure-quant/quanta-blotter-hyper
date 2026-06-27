(ns antman.simulator-page-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [antman.simulator.signal :as sim-signal]))

(deftest load-signal-test
  (testing "loads trade from signal.json"
    (let [trade (sim-signal/load-signal)]
      (is (= "EURUSD-20260519-001" (:trade-id trade)))
      (is (= "EURUSD" (:symbol trade)))
      (is (= 3 (count (:take-profits trade)))))))

(deftest assemble-trade-test
  (testing "reconstructs nested trade map from field values"
    (let [trade (sim-signal/assemble-trade
                 {[:trade-id] "TEST-001"
                  [:symbol] "GBPUSD"
                  [:net-r] ""
                  [:take-profits] [{:level 1 :price 1.2 :percent 50 :hit true}]
                  [:risk-decision :outcome] "Approved"
                  [:risk-decision :violations] "[]"})]
      (is (= "TEST-001" (:trade-id trade)))
      (is (= "GBPUSD" (:symbol trade)))
      (is (nil? (:net-r trade)))
      (is (= [{:level 1 :price 1.2 :percent 50 :hit true}] (:take-profits trade)))
      (is (= [] (get-in trade [:risk-decision :violations]))))))
