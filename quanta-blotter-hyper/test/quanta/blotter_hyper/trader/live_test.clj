(ns quanta.blotter-hyper.trader.live-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [quanta.blotter-hyper.trader.live :as live]))

(def sample-state
  {:open-positions [{:position/trader "florian" :position/asset "BTC"}
                    {:position/trader "arne" :position/asset "ETH"}]
   :working-orders [{:order/trader "florian" :order/id 1}
                    {:order/trader "arne" :order/id 2}]})

(deftest filter-for-trader-test
  (testing "keeps only rows matching the current trader"
    (let [[filtered] (into [] (#'live/filter-for-trader "florian") [sample-state])]
      (is (= [{:position/trader "florian" :position/asset "BTC"}]
             (:open-positions filtered)))
      (is (= [{:order/trader "florian" :order/id 1}]
             (:working-orders filtered)))))

  (testing "returns empty collections when trader has no rows"
    (let [[filtered] (into [] (#'live/filter-for-trader "ant") [sample-state])]
      (is (= [] (:open-positions filtered)))
      (is (= [] (:working-orders filtered))))))
