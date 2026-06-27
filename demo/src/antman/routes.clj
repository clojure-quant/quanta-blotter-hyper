(ns antman.routes
  (:require
   [antman.auth :as auth]
   [antman.token :as token]
   [antman.ui.highcharts-random-page :refer [highcharts-random-page]]
   [antman.ui.layout-page :refer [layout-page]]
   [antman.ui.panels :refer [positions-panel trades-panel]]
   [antman.ui.quotelist-page :refer [quotelist-page]]
   [antman.ui.simulator-page :refer [simulator-page]]
   [antman.ui.trading :refer [trading-page]]))

(defn- with-roles
  [required-roles route-data]
  (assoc route-data :render-middleware [(auth/wrap-require-roles required-roles)]))

(def app-routes
  [["/" {:name :home
         :title "Ant Man"
         :hyper/disabled? true
         :get (fn [req]
                (if (auth/signed-in? req)
                  {:status 302 :headers {"Location" "/trading"} :body ""}
                  {:status 302 :headers {"Location" "/login"} :body ""}))}]
   ["/trading"
    (with-roles #{:trader}
     {:name :trading
      :title "Trading"
      :get #'trading-page})]
   ["/layout"
    (with-roles #{:trader}
     {:name :layout
      :title "Layout"
      :get #'layout-page})]
   ["/highcharts-random"
    (with-roles #{:trader}
     {:name :highcharts-random
      :title "Highcharts random"
      :get #'highcharts-random-page})]
   ["/panels/positions"
    (with-roles #{:trader}
     {:name :panel-positions
      :title "Positions"
      :get #'positions-panel})]
   ["/panels/trades"
    (with-roles #{:trader}
     {:name :panel-trades
      :title "Trades"
      :get #'trades-panel})]
   ["/quotelist"
    (with-roles #{:viewer :trader}
     {:name :quotelist
      :title "Quote list"
      :get #'quotelist-page})]
   ["/simulator"
    (with-roles #{:admin}
     {:name :simulator
      :title "Signal simulator"
      :get #'simulator-page})]])

(defonce all-routes
  (into (vec app-routes) (token/routes nil)))

(defn rebuild!
  [token]
  (alter-var-root #'all-routes
                  (constantly (into (vec app-routes) (token/routes token)))))
