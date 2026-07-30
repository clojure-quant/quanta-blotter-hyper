(ns quanta.blotter-hyper.missionary
  (:require
   [taoensso.timbre :as log]))

(defmacro start-task!
  "Starts a Missionary task and logs its completion, cancellation, or failure."
  [task label]
  (let [task-sym (gensym "task")
        label-sym (gensym "label")
        result-sym (gensym "result")
        failure-sym (gensym "failure")
        done-form (log/keep-callsite
                   `(log/info ~label-sym "done" ~result-sym))
        cancelled-form (log/keep-callsite
                        `(log/info ~label-sym "cancelled"))
        error-form (log/keep-callsite
                    `(log/error ~failure-sym ~label-sym "error"))]
    `(let [~task-sym ~task
           ~label-sym ~label]
       (~task-sym
        (fn [~result-sym]
          ~done-form)
        (fn [~failure-sym]
          (if (instance? missionary.Cancelled ~failure-sym)
            ~cancelled-form
            ~error-form))))))
