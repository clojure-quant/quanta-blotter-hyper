(ns antman.demo.quotelist
  (:require
   [missionary.core :as m]
   [quanta.quote.account-manager :refer [create-account-manager add-edn-accounts quote-list-dict-flow]]
   [quanta.blotter.logger :refer [create-logger log start-log-flow-to-logger]]))




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
                           (fn [s v]
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
  @quotelist
  am
  (dispose!)
 ; 
  )
