(ns antman.demo.quotelist
  (:require
   [missionary.core :as m]
   [quanta.missionary.logger :refer [create-logger log]]
   [quanta.quote.account-manager :refer [create-account-manager add-edn-accounts quote-list-dict-flow]]
   ))




(def am
  (let [l (create-logger "log/quotes.txt" false)
        log-fn (partial log l)

        am (create-account-manager log-fn)
        _ (add-edn-accounts am "demo-quote-accounts.edn")] am))

(defn create-quotelist []
  (let [ql (quote-list-dict-flow am (fn [_asset] 1)
                                 ["EURUSD" "USDJPY" "EURNOK"])
        quotelist (atom {})
        quote-processor   (m/reduce
                           (fn [_ v]
                             ;(println "QUOTELIST: " v)
                             (reset! quotelist v)
                             nil)
                           nil ql)
        dispose! (quote-processor #(println "1-quote-printer done " %) 
                                  #(println "1-quote-printer CRASH " %))
        ]
    {:dispose! dispose! 
     :quotelist quotelist}))

    


(comment
  (def q (create-quotelist))
  @(:quotelist q)
  am
  ((:dispose! q)))
