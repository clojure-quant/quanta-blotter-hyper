(ns quanta.blotter-hyper.component.instant-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [quanta.blotter-hyper.component.instant :as instant]))

(deftest truncate-instant-test
  (let [date (instant/truncate-instant "2020-05-27T18:07:07.987Z")]
    (is (instance? java.util.Date date))
    (is (zero? (mod (.getTime date) 1000)))
    (is (= "2020-05-27T18:07:07Z"
           (instant/format-instant-seconds date)))))

(deftest instant-presets-return-dates
  (testing "known presets return whole-second Dates"
    (doseq [key ["yesterday-midnight" "yesterday-same" "7-days-ago"]]
      (let [date (instant/apply-preset key)]
        (is (instance? java.util.Date date))
        (is (zero? (mod (.getTime date) 1000))))))
  (testing "unknown presets remain nil"
    (is (nil? (instant/apply-preset "unknown")))))
