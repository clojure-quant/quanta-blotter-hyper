(ns quanta.blotter-hyper.trader.backoffice
  (:require
   [hyper.core :as h]
   ;[antman.ui.components :as ui]
   ))

(defn backoffice-page
  [{:keys [db] :as _req}]
  (println "BACKOFFICE-PAGE")
  (h/view
   {:mount (fn []
             (println "MOUNTING BACKOFFICE-PAGE")
             (let [r "x"]
               ;(h/watch! (:quotelist r))
               r))
    :render (fn [{:keys [xxx]} req]
              [:motion.div.quotelist-page
               ;(ui/nav)
               [:h1 "Backoffice"]
               ;(ui/quotelist-table @quotelist)
               
               ])
    :unmount (fn [{:keys [xxx dispose!]}]
               (println "UNMOUNTING Backoffice-PAGE")
               (dispose!))}))

