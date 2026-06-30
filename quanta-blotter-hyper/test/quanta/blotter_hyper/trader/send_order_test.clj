(ns quanta.blotter-hyper.trader.send-order-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]
   [quanta.blotter-hyper.trader.send-order :as send-order]
   [quanta.blotter.oms.db :as db]
   [quanta.util.datahike :as datahike]))

(def demo-accounts-path "../demo/demo-accounts.edn")

(defn- load-demo-accounts []
  (-> demo-accounts-path slurp edn/read-string))

(deftest available-assets-test
  (is (= ["EURUSD" "USDJPY" "BTCUSDT.LF.BB"]
         (send-order/available-assets))))

(deftest trader-accounts-test
  (let [edn-accounts (load-demo-accounts)
        conn (datahike/db-start-mem db/schema)]
    (try
      (doseq [account edn-accounts]
        (db/create-account conn (select-keys account [:account/id :account/trader :account/api]))
        (db/update-account conn (select-keys account [:account/id :account/name])))
      (doseq [account edn-accounts]
        (db/enable-account conn (:account/id account) true))
      (db/enable-account conn 2 false)
      (testing "returns enabled account id to name map for trader"
        (is (= {1 "fpaper-2fills"
                4 "fpaper-reject"
                5 "fpaper fast/bad"
                6 "fpaper corrupt"
                1000 "pepperstone demo1"}
               (send-order/trader-accounts conn "florian"))))
      (finally
        (datahike/db-stop conn)))))

(deftest state->order-details-test
  (testing "limit order includes limit"
    (let [state (assoc (send-order/default-state 1) :order-type :limit :limit 1.1035M)]
      (is (= {:account/id 1
              :order-id (:order-id state)
              :asset "EURUSD"
              :side :buy
              :order-type :limit
              :limit 1.1035M
              :qty 10000M
              :campaign "manual order"
              :label :manual}
             (send-order/state->order-details state)))))

  (testing "market order omits limit"
    (let [state (assoc (send-order/default-state 1) :order-type :market)]
      (is (not (contains? (send-order/state->order-details state) :limit)))))

  (testing "default state is a valid new-order"
    (is (send-order/valid-new-order? (send-order/default-state 1))))

  (testing "invalid state returns validation error map"
    (let [state (assoc (send-order/default-state 1) :qty -1M)]
      (is (false? (send-order/valid-new-order? state)))
      (is (map? (send-order/validation-error state)))
      (is (contains? (send-order/validation-error state) :qty)))))
