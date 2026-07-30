(ns quanta.blotter-hyper.trader.send-order-test
  (:require
   [clojure.edn :as edn]
   [clojure.test :refer [deftest is testing]]
   [datahike.api :as d]
   [quanta.blotter-hyper.trader.send-order :as send-order]
   [quanta.blotter.oms.db :as db]
   [quanta.util.datahike :as datahike]))

(def demo-accounts-path "../demo/demo-accounts.edn")

(defn- load-demo-accounts []
  (-> demo-accounts-path slurp edn/read-string))

(defn- seed-list!
  [conn list-name assets]
  (d/transact conn [{:lists/name list-name
                     :lists/asset (map-indexed vector assets)}])
  (d/q '[:find ?e .
         :in $ ?name
         :where [?e :lists/name ?name]]
       @conn list-name))

(deftest trader-accounts-test
  (let [edn-accounts (load-demo-accounts)
        conn (datahike/db-start-mem db/schema)]
    (try
      (let [list-ids {"test" (seed-list! conn "test" ["__TEST" "__TEST2"])
                      "spot-fx" (seed-list! conn "spot-fx" ["EURUSD" "USDJPY"])
                      "crypto-test" (seed-list! conn "crypto-test" ["BTCUSDT.S.BBT"
                                                                   "BTCUSDT.LF.BBT"])}]
      (doseq [account edn-accounts]
        (db/create-account conn (select-keys account [:account/id :account/trader :account/api]))
        (db/update-account conn
                           {:account/id (:account/id account)
                            :account/name (:account/name account)
                            :account/asset-list (get list-ids (:account/asset-list account))}))
      (doseq [account edn-accounts]
        (db/enable-account conn (:account/id account) true))
      (db/enable-account conn 2 false)
      (testing "returns enabled accounts with ordered asset-list symbols"
        (is (= [{:account/id 1
                 :account/name "fpaper-2fills"
                 :account/assets ["__TEST" "__TEST2"]}
                {:account/id 4
                 :account/name "fpaper-reject"
                 :account/assets ["__TEST" "__TEST2"]}
                {:account/id 5
                 :account/name "fpaper fast/bad"
                 :account/assets ["__TEST" "__TEST2"]}
                {:account/id 6
                 :account/name "fpaper corrupt"
                 :account/assets ["__TEST" "__TEST2"]}
                {:account/id 1000
                 :account/name "pepperstone demo1"
                 :account/assets ["EURUSD" "USDJPY"]}
                {:account/id 2000
                 :account/name "bybit florian demo1"
                 :account/assets ["BTCUSDT.S.BBT" "BTCUSDT.LF.BBT"]}]
               (send-order/trader-accounts conn "florian")))))
      (finally
        (datahike/db-stop conn)))))

(deftest select-account-state-test
  (let [accounts [{:account/id 1 :account/assets ["EURUSD" "USDJPY"]}
                  {:account/id 2 :account/assets ["BTCUSDT.S.BB"]}
                  {:account/id 3 :account/assets []}]]
    (testing "keeps an asset allowed by the new account"
      (is (= {:account 1 :asset "USDJPY"}
             (send-order/select-account-state
              {:account 2 :asset "USDJPY"} accounts 1))))
    (testing "falls back to the new account's first asset"
      (is (= {:account 2 :asset "BTCUSDT.S.BB"}
             (send-order/select-account-state
              {:account 1 :asset "EURUSD"} accounts 2))))
    (testing "uses nil when the account has no assets"
      (is (= {:account 3 :asset nil}
             (send-order/select-account-state
              {:account 1 :asset "EURUSD"} accounts 3))))))

(deftest state->order-details-test
  (testing "limit order includes limit"
    (let [state (assoc (send-order/default-state 1 "EURUSD") :order-type :limit :limit 1.1035M)]
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
    (let [state (assoc (send-order/default-state 1 "EURUSD") :order-type :market)]
      (is (not (contains? (send-order/state->order-details state) :limit)))))

  (testing "position-id is optional and included when entered"
    (let [state (send-order/default-state 1 "EURUSD")]
      (is (not (contains? (send-order/state->order-details state) :position-id)))
      (is (= "hedge-42"
             (:position-id
              (send-order/state->order-details
               (assoc state :position-id "hedge-42")))))))

  (testing "default state is a valid new-order"
    (is (send-order/valid-new-order? (send-order/default-state 1 "EURUSD"))))

  (testing "invalid state returns validation error map"
    (let [state (assoc (send-order/default-state 1 "EURUSD") :qty -1M)]
      (is (false? (send-order/valid-new-order? state)))
      (is (map? (send-order/validation-error state)))
      (is (contains? (send-order/validation-error state) :qty)))))

(deftest cancel-order-test
  (testing "cancel details use cancel account, asset, and cancel order-id"
    (let [state (assoc (send-order/default-state 1 "EURUSD")
                        :cancel-account 1000
                        :cancel-order-id "abc123"
                        :asset "USDJPY")]
      (is (= {:account/id 1000
              :order-id "abc123"
              :asset "USDJPY"}
             (send-order/state->cancel-details state)))))

  (testing "valid cancel order"
    (let [state (assoc (send-order/default-state 1 "EURUSD") :cancel-order-id "abc123")]
      (is (send-order/valid-cancel-order? state))))

  (testing "empty cancel order-id is invalid"
    (is (not (send-order/valid-cancel-order? (send-order/default-state 1 "EURUSD")))))

  (testing "empty cancel order-id returns error message"
    (is (= "order-id required"
           (send-order/cancel-validation-error (send-order/default-state 1 "EURUSD")))))

  (testing "working order maps to cancel details"
    (let [order {:order/account-id 1000
                 :order/id "abc123"
                 :order/asset "EURUSD"}
          state (send-order/order->cancel-state order)]
      (is (= {:account/id 1000
              :order-id "abc123"
              :asset "EURUSD"}
             (send-order/state->cancel-details state)))
      (is (send-order/valid-cancel-order? state)))))
