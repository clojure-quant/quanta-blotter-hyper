(ns antman.sim-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [antman.sim.generate :as gen]
   [antman.sim.state :as state]))

(deftest position-generate-test
  (testing "position has required keys"
    (let [p (gen/position)]
      (is (contains? p :broker))
      (is (contains? p :asset))
      (is (contains? p :entry))
      (is (contains? p :price))
      (is (contains? p :pl))
      (is (contains? p :tp))
      (is (contains? p :sl)))))

(deftest trade-generate-test
  (testing "trade has required keys"
    (let [t (gen/trade)]
      (is (contains? t :broker))
      (is (contains? t :side))
      (is (contains? t :qty))
      (is (contains? t :time)))))

(deftest apply-position-tick-test
  (testing "tick updates or adds positions"
    (let [seed [{:id "1" :broker "IBKR" :asset "AAPL" :entry 100.0 :price 101.0 :pl 1.0 :tp 110.0 :sl 95.0}]
          next (state/apply-position-tick! seed)]
      (is (seq next))
      (is (<= (count seed) (count next))))))

(deftest position-update-flash-test
  (testing "price-flash reflects direction of price move"
    (with-redefs [rand (constantly 1.0)]
      (is (= :up (:price-flash (gen/position-update {:price 100.0 :entry 90.0})))))
    (with-redefs [rand (constantly 0.0)]
      (is (= :down (:price-flash (gen/position-update {:price 100.0 :entry 90.0})))))))
