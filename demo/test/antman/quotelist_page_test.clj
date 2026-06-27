(ns antman.quotelist-page-test
  (:require
   [clojure.string :as str]
   [clojure.test :refer [deftest is testing]]
   [hyper.core :as h]
   [hyper.test :as ht]
   [antman.demo.quotelist :as quotelist]
   [antman.ui.components :as ui]))

(deftest quotelist-table-test
  (testing "renders quote dictionary rows"
    (let [html (ht/test-page
                (fn [_]
                  (ui/quotelist-table
                   {"EURUSD" {:account 1 :asset "EURUSD" :bid 1.08 :ask 1.09}
                    "USDJPY" {:account 1 :asset "USDJPY" :bid 150.0 :ask 150.01}})))]
      (is (str/includes? (:body-html html) "EURUSD"))
      (is (str/includes? (:body-html html) "1.08"))
      (is (str/includes? (:body-html html) "150.01")))))

(deftest quotelist-table-empty-test
  (testing "shows placeholder when dictionary is empty"
    (let [html (ht/test-page (fn [_] (ui/quotelist-table {})))]
      (is (str/includes? (:body-html html) "No quotes yet")))))

(deftest quotelist-page-watch-test
  (testing "registers quotelist atom via h/watch!"
    (let [result (ht/test-page
                  (fn [_]
                    (h/watch! quotelist/quotelist)
                    [:div]))]
      (is (= 1 (count (:watches result))))
      (is (identical? quotelist/quotelist (first (:watches result)))))))

(deftest quotelist-page-reactive-test
  (testing "page reads from quotelist atom"
    (reset! quotelist/quotelist
            {"EURUSD" {:account 1 :asset "EURUSD" :bid 1.08 :ask 1.09}})
    (let [html (ht/test-page
                (fn [_]
                  [:div (ui/quotelist-table @quotelist/quotelist)]))]
      (is (str/includes? (:body-html html) "EURUSD"))
      (is (str/includes? (:body-html html) "1.08")))))
