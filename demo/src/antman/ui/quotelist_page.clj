(ns antman.ui.quotelist-page
  (:require
   [hyper.core :as h]
   [antman.demo.quotelist :as quotelist]
   [antman.ui.components :as ui]))

(defn quotelist-page
  [_req]
  ;(println "QUOTELIST-PAGE")
  (h/view
   {:mount (fn []
             (println "MOUNTING QUOTELIST-PAGE") 
             (let [r (quotelist/create-quotelist)]
               (h/watch! (:quotelist r))
               r
               )
             )
    :render (fn [{:keys [quotelist]} req]
              [:motion.div.quotelist-page
               (ui/nav)
               [:h1 "Quote list"]
               (ui/quotelist-table @quotelist)])
    :unmount (fn [{:keys [quotelist dispose!]}]
               (println "UNMOUNTING QUOTELIST-PAGE")
               (dispose!))}))

