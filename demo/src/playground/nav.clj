(ns playground.nav
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.status.view :refer [sse-connection-status]]))

(defn nav
  []
  [:nav.app-nav
   (sse-connection-status)
   [:a (h/navigate :playground/layout) "Layout"]
   " · "
   [:a (h/navigate :playground/highcharts-random) "Highcharts random"]
   " · "
   [:a (h/navigate :playground/simulator) "Simulator"]
   " · "
   [:a (h/navigate :playground/notifications) "Notifications"]
   " · "
   [:a {:href "/me"} "User"]])
