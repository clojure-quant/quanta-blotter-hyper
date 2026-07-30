(ns quanta.blotter-hyper.missionary-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [quanta.blotter-hyper.missionary :refer [start-task!]]
   [taoensso.timbre :as log])
  (:import
   [missionary Cancelled]))

(defn- capture-logs [f]
  (let [events (atom [])
        result (log/with-config
                 {:min-level :trace
                  :appenders {:capture {:enabled? true
                                        :async? false
                                        :fn #(swap! events conj %)}}}
                 (f))]
    {:events @events
     :result result}))

(deftest success-logging-test
  (testing "logs success, returns the disposer, and evaluates arguments once"
    (let [task-evaluations (atom 0)
          label-evaluations (atom 0)
          task (fn [success _failure]
                 (success :result)
                 :dispose)
          {:keys [events result]}
          (capture-logs
           #(start-task! (do (swap! task-evaluations inc) task)
                         (do (swap! label-evaluations inc) "worker")))
          event (first events)]
      (is (= :dispose result))
      (is (= 1 @task-evaluations))
      (is (= 1 @label-evaluations))
      (is (= :info (:level event)))
      (is (= ["worker" "done" :result] (:vargs event)))
      (is (= "quanta.blotter-hyper.missionary-test" (:?ns-str event))))))

(deftest cancellation-logging-test
  (testing "logs Missionary cancellation as info"
    (let [cancelled (Cancelled. "stopped")
          {:keys [events]}
          (capture-logs
           #(start-task! (fn [_success failure]
                           (failure cancelled))
                         "worker"))
          event (first events)]
      (is (= :info (:level event)))
      (is (= ["worker" "cancelled"] (:vargs event)))
      (is (nil? (:?err event))))))

(deftest failure-logging-test
  (testing "logs other failures as errors with the throwable retained"
    (let [failure (ex-info "failed" {})
          {:keys [events]}
          (capture-logs
           #(start-task! (fn [_success fail]
                           (fail failure))
                         "worker"))
          event (first events)]
      (is (= :error (:level event)))
      (is (= ["worker" "error"] (:vargs event)))
      (is (identical? failure (:?err event))))))
