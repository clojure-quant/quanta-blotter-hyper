(ns quanta.blotter-hyper.view.orders-trades-campaign-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [tick.core :as t]
   [quanta.blotter.oms.db :as db]
   [quanta.blotter-hyper.view.common :as common]
   [quanta.blotter-hyper.view.orders :as orders-view]
   [quanta.blotter-hyper.view.positions :as positions-view]
   [quanta.blotter-hyper.view.trades :as trades-view]
   [quanta.util.datahike :as datahike]))

(defn- fresh-db []
  (datahike/db-start-mem db/schema))

(defn- seed-account!
  [conn account-id trader name]
  (db/create-account conn {:account/id account-id
                           :account/trader trader
                           :account/api :paper})
  (db/update-account conn {:account/id account-id
                           :account/name name})
  (db/enable-account conn account-id true))

(def ^:private now (t/instant))

(defn- sample-order
  [id account-id asset campaign]
  (cond-> {:order/id id
           :order/account-id account-id
           :order/asset asset
           :order/side :buy
           :order/type :limit
           :order/status :working
           :order/qty 1.0M
           :order/qty-filled 0.0M
           :order/qty-working 1.0M
           :order/avg-price nil
           :order/date now
           :order/history []
           :order/label :manual}
    campaign (assoc :order/campaign campaign)))

(defn- sample-fill
  [id order-id account-id asset campaign]
  (cond-> {:fill/id id
           :fill/order-id order-id
           :fill/account-id account-id
           :fill/asset asset
           :fill/side :buy
           :fill/qty 1.0M
           :fill/price 1.1M
           :fill/date now
           :fill/label :manual}
    campaign (assoc :fill/campaign campaign)))

(defn- sample-position
  [account-id asset]
  {:position/account account-id
   :position/asset asset
   :position/side :long
   :position/open true
   :position/qty-open 1.0M
   :position/qty 1.0M
   :position/average-entry-price 1.1M
   :position/realized-pl 0.0M
   :position/avg-exit-price nil
   :position/date-open now})

(defn- order-ids [rows]
  (set (map :order/id rows)))

(defn- fill-ids [rows]
  (set (map :fill/id rows)))

(defn- position-assets [rows]
  (set (map :position/asset rows)))

(deftest query-orders-with-and-without-campaign
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (db/process conn state [:order (sample-order "o1" 1 "EURUSD" "fx-q2")])
      (db/process conn state [:order (sample-order "o2" 1 "EURUSD" "scalp")])
      (db/process conn state [:order (sample-order "o3" 1 "EURUSD" nil)])
      (testing "without campaign returns all orders"
        (is (= #{"o1" "o2" "o3"}
               (order-ids (orders-view/query-orders conn {:trader nil}))))
        (is (= #{"o1" "o2" "o3"}
               (order-ids (orders-view/query-orders conn {:trader nil :campaign ""})))))
      (testing "with campaign returns only matching orders"
        (let [rows (orders-view/query-orders conn {:trader nil :campaign "fx-q2"})]
          (is (= #{"o1"} (order-ids rows)))
          (is (= "fx-q2" (:order/campaign (first rows)))))
        (is (= #{"o2"}
               (order-ids (orders-view/query-orders conn {:trader nil :campaign "scalp"}))))
        (is (= #{}
               (order-ids (orders-view/query-orders conn {:trader nil :campaign "missing"})))))
      (testing "campaign filter matches substrings"
        (is (= #{"o1"}
               (order-ids (orders-view/query-orders conn {:trader nil :campaign "fx"}))))
        (is (= #{"o1"}
               (order-ids (orders-view/query-orders conn {:trader nil :campaign "f"}))))
        (is (= #{"o2"}
               (order-ids (orders-view/query-orders conn {:trader nil :campaign "cal"})))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-fills-with-and-without-campaign
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (db/process conn state [:fill (sample-fill "f1" "o1" 1 "EURUSD" "fx-q2")])
      (db/process conn state [:fill (sample-fill "f2" "o2" 1 "EURUSD" "scalp")])
      (db/process conn state [:fill (sample-fill "f3" "o3" 1 "EURUSD" nil)])
      (testing "without campaign returns all fills"
        (is (= #{"f1" "f2" "f3"}
               (fill-ids (trades-view/query-fills conn {:trader nil}))))
        (is (= #{"f1" "f2" "f3"}
               (fill-ids (trades-view/query-fills conn {:trader nil :campaign ""})))))
      (testing "with campaign returns only matching fills"
        (let [rows (trades-view/query-fills conn {:trader nil :campaign "fx-q2"})]
          (is (= #{"f1"} (fill-ids rows)))
          (is (= "fx-q2" (:fill/campaign (first rows)))))
        (is (= #{"f2"}
               (fill-ids (trades-view/query-fills conn {:trader nil :campaign "scalp"}))))
        (is (= #{}
               (fill-ids (trades-view/query-fills conn {:trader nil :campaign "missing"})))))
      (testing "campaign filter matches substrings"
        (is (= #{"f1"}
               (fill-ids (trades-view/query-fills conn {:trader nil :campaign "fx"}))))
        (is (= #{"f1"}
               (fill-ids (trades-view/query-fills conn {:trader nil :campaign "f"}))))
        (is (= #{"f2"}
               (fill-ids (trades-view/query-fills conn {:trader nil :campaign "cal"})))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-orders-asset-substring
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (db/process conn state [:order (sample-order "o1" 1 "EURUSD" nil)])
      (db/process conn state [:order (sample-order "o2" 1 "EURJPY" nil)])
      (db/process conn state [:order (sample-order "o3" 1 "NZDEUR" nil)])
      (db/process conn state [:order (sample-order "o4" 1 "USDJPY" nil)])
      (testing "without asset returns all"
        (is (= #{"o1" "o2" "o3" "o4"}
               (order-ids (orders-view/query-orders conn {:trader nil :asset ""})))))
      (testing "asset substring matches any position"
        (is (= #{"o1" "o2" "o3"}
               (order-ids (orders-view/query-orders conn {:trader nil :asset "EUR"}))))
        (is (= #{"o2" "o4"}
               (order-ids (orders-view/query-orders conn {:trader nil :asset "JPY"}))))
        (is (= #{}
               (order-ids (orders-view/query-orders conn {:trader nil :asset "BTC"})))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-fills-asset-substring
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (db/process conn state [:fill (sample-fill "f1" "o1" 1 "EURUSD" nil)])
      (db/process conn state [:fill (sample-fill "f2" "o2" 1 "EURJPY" nil)])
      (db/process conn state [:fill (sample-fill "f3" "o3" 1 "NZDEUR" nil)])
      (db/process conn state [:fill (sample-fill "f4" "o4" 1 "USDJPY" nil)])
      (testing "without asset returns all"
        (is (= #{"f1" "f2" "f3" "f4"}
               (fill-ids (trades-view/query-fills conn {:trader nil :asset ""})))))
      (testing "asset substring matches any position"
        (is (= #{"f1" "f2" "f3"}
               (fill-ids (trades-view/query-fills conn {:trader nil :asset "EUR"}))))
        (is (= #{"f2" "f4"}
               (fill-ids (trades-view/query-fills conn {:trader nil :asset "JPY"})))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-positions-asset-substring
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (db/process conn state [:position (sample-position 1 "EURUSD")])
      (db/process conn state [:position (sample-position 1 "EURJPY")])
      (db/process conn state [:position (sample-position 1 "NZDEUR")])
      (db/process conn state [:position (sample-position 1 "USDJPY")])
      (testing "without asset returns all"
        (is (= #{"EURUSD" "EURJPY" "NZDEUR" "USDJPY"}
               (position-assets (positions-view/query-positions conn {:trader nil :asset ""})))))
      (testing "asset substring matches any position"
        (is (= #{"EURUSD" "EURJPY" "NZDEUR"}
               (position-assets (positions-view/query-positions conn {:trader nil :asset "EUR"}))))
        (is (= #{"EURJPY" "USDJPY"}
               (position-assets (positions-view/query-positions conn {:trader nil :asset "JPY"})))))
      (finally
        (datahike/db-stop conn)))))

(deftest parse-account-id-test
  (is (= 1 (common/parse-account-id "1")))
  (is (= 1000 (common/parse-account-id " 1000 ")))
  (is (nil? (common/parse-account-id "")))
  (is (nil? (common/parse-account-id "flo")))
  (is (nil? (common/parse-account-id "1.5"))))

(deftest query-orders-account-id-filter
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (seed-account! conn 2 "florian" "a2")
      (db/process conn state [:order (sample-order "o1" 1 "EURUSD" nil)])
      (db/process conn state [:order (sample-order "o2" 2 "EURUSD" nil)])
      (testing "without account-id returns all"
        (is (= #{"o1" "o2"}
               (order-ids (orders-view/query-orders conn {:trader nil})))))
      (testing "with account-id returns only that account"
        (is (= #{"o1"}
               (order-ids (orders-view/query-orders conn {:trader nil :account-id 1}))))
        (is (= #{"o2"}
               (order-ids (orders-view/query-orders conn {:trader nil :account-id 2}))))
        (is (= #{}
               (order-ids (orders-view/query-orders conn {:trader nil :account-id 99})))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-fills-account-id-filter
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (seed-account! conn 2 "florian" "a2")
      (db/process conn state [:fill (sample-fill "f1" "o1" 1 "EURUSD" nil)])
      (db/process conn state [:fill (sample-fill "f2" "o2" 2 "EURUSD" nil)])
      (is (= #{"f1"}
             (fill-ids (trades-view/query-fills conn {:trader nil :account-id 1}))))
      (is (= #{"f2"}
             (fill-ids (trades-view/query-fills conn {:trader nil :account-id 2}))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-positions-account-id-filter
  (let [conn (fresh-db)
        state (db/new-state)]
    (try
      (seed-account! conn 1 "florian" "a1")
      (seed-account! conn 2 "florian" "a2")
      (db/process conn state [:position (sample-position 1 "EURUSD")])
      (db/process conn state [:position (sample-position 2 "USDJPY")])
      (is (= #{"EURUSD"}
             (position-assets (positions-view/query-positions conn {:trader nil :account-id 1}))))
      (is (= #{"USDJPY"}
             (position-assets (positions-view/query-positions conn {:trader nil :account-id 2}))))
      (finally
        (datahike/db-stop conn)))))

(deftest query-minimum-date-filter
  "Orders (:order/date), trades (:fill/date), and positions (:position/date-open)
  respect :start-date as an inclusive minimum."
  (let [conn (fresh-db)
        state (db/new-state)
        older (t/instant "2020-01-01T00:00:00Z")
        at-cutoff (t/instant "2024-01-01T00:00:00Z")
        newer (t/instant "2024-06-15T12:00:00Z")
        cutoff at-cutoff]
    (try
      (seed-account! conn 1 "florian" "a1")
      (db/process conn state [:order (assoc (sample-order "o-old" 1 "EURUSD" nil)
                                            :order/date older)])
      (db/process conn state [:order (assoc (sample-order "o-cut" 1 "EURUSD" nil)
                                            :order/date at-cutoff)])
      (db/process conn state [:order (assoc (sample-order "o-new" 1 "GBPUSD" nil)
                                            :order/date newer)])
      (db/process conn state [:fill (assoc (sample-fill "f-old" "o-old" 1 "EURUSD" nil)
                                           :fill/date older)])
      (db/process conn state [:fill (assoc (sample-fill "f-cut" "o-cut" 1 "EURUSD" nil)
                                           :fill/date at-cutoff)])
      (db/process conn state [:fill (assoc (sample-fill "f-new" "o-new" 1 "GBPUSD" nil)
                                           :fill/date newer)])
      (db/process conn state [:position (assoc (sample-position 1 "EURUSD")
                                               :position/date-open older)])
      (db/process conn state [:position (assoc (sample-position 1 "USDJPY")
                                               :position/date-open at-cutoff)])
      (db/process conn state [:position (assoc (sample-position 1 "GBPUSD")
                                               :position/date-open newer)])
      (testing "without minimum-date returns all"
        (is (= #{"o-old" "o-cut" "o-new"}
               (order-ids (orders-view/query-orders conn {:trader nil :start-date nil}))))
        (is (= #{"f-old" "f-cut" "f-new"}
               (fill-ids (trades-view/query-fills conn {:trader nil}))))
        (is (= #{"EURUSD" "USDJPY" "GBPUSD"}
               (position-assets (positions-view/query-positions conn {:trader nil})))))
      (testing "minimum-date keeps on/after (inclusive)"
        (is (= #{"o-cut" "o-new"}
               (order-ids (orders-view/query-orders conn {:trader nil :start-date cutoff}))))
        (is (= #{"f-cut" "f-new"}
               (fill-ids (trades-view/query-fills conn {:trader nil :start-date cutoff}))))
        (is (= #{"USDJPY" "GBPUSD"}
               (position-assets (positions-view/query-positions conn {:trader nil :start-date cutoff})))))
      (finally
        (datahike/db-stop conn)))))
