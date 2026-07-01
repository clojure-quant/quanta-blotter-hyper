(ns quanta.blotter-hyper.trader.quotes-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [quanta.blotter-hyper.view.quotes :as quotes-view]))

(deftest quotes-table-test
  (testing "renders quote rows with print-table columns"
    (let [html (quotes-view/quotes-table {"EURUSD" {:account 1
                                                    :asset "EURUSD"
                                                    :bid 1.1M
                                                    :ask 1.2M
                                                    :ts #inst "2026-01-01T00:00:00.000Z"}
                                          "USDJPY" {:account 4
                                                    :asset "USDJPY"
                                                    :bid 150M
                                                    :ask 151M
                                                    :ts nil}})]
      (is (vector? html))
      (is (= :div.quotes-table-wrap (first html)))
      (is (re-find #"EURUSD" (str html)))
      (is (re-find #"USDJPY" (str html))))))

(deftest enrich-quotes-with-flash-test
  (testing "first update has no flash"
    (let [[enriched _] (quotes-view/enrich-quotes-with-flash
                        {"EURUSD" {:bid 1.1M :ask 1.2M}}
                        {})]
      (is (nil? (:bid-flash (get enriched "EURUSD"))))
      (is (nil? (:ask-flash (get enriched "EURUSD"))))))

  (testing "bid/ask flash reflects direction of move"
    (let [[enriched _] (quotes-view/enrich-quotes-with-flash
                        {"EURUSD" {:bid 1.2M :ask 1.19M}}
                        {"EURUSD" {:bid 1.1M :ask 1.2M}})]
      (is (= :up (:bid-flash (get enriched "EURUSD"))))
      (is (= :down (:ask-flash (get enriched "EURUSD")))))))

(deftest quotes-table-flash-class-test
  (testing "renders price-flash classes on bid/ask cells"
    (let [html (quotes-view/quotes-table
                {"EURUSD" {:account 1 :asset "EURUSD"
                           :bid 1.2M :ask 1.19M
                           :bid-flash :up :ask-flash :down}})]
      (is (re-find #"price-flash-up" (str html)))
      (is (re-find #"price-flash-down" (str html))))))

(deftest quotes-table-precision-test
  (testing "bid/ask show full precision, not fixed decimals"
    (let [html (str (quotes-view/quotes-table
                     {"USDJPY" {:account 1 :asset "USDJPY"
                                :bid 150.123M :ask 150.456789M}}))]
      (is (re-find #"150\.123" html))
      (is (re-find #"150\.456789" html)))))
