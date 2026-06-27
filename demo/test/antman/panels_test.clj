(ns antman.panels-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [hyper.test :as ht]
   [antman.sim.generate :as gen]
   [antman.sim.state :as sim]
   [antman.ui.panels :refer [positions-panel trades-panel]]))

(deftest positions-panel-test
  (testing "renders positions table"
    (reset! sim/positions* (gen/seed-positions 2))
    (let [result (ht/test-page positions-panel)]
      (is (str/includes? (:body-html result) "Positions"))
      (is (str/includes? (:body-html result) "sse-connection-status"))
      (is (str/includes? (:body-html result) "Broker"))
      (is (str/includes? (:body-html result) "P/L")))))

(deftest trades-panel-test
  (testing "renders trades table"
    (reset! sim/trades* [(gen/trade)])
    (let [result (ht/test-page trades-panel)]
      (is (str/includes? (:body-html result) "Trades"))
      (is (str/includes? (:body-html result) "Side")))))
