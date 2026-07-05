(ns playground.routes
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.token :as token]
   [quanta.blotter-hyper.trader.routes :as trader-routes]
   [quanta.blotter-hyper.admin.routes :as admin-routes]
   [playground.nav :refer [nav]]
   [playground.golden-layout.page :refer [layout-page]]
   [playground.highcharts.page-random :refer [highcharts-random-page]]
   [playground.notification.view :refer [notifications-page]]
   [playground.simulator.page :refer [simulator-page]]
   [playground.winbox.page :refer [winbox-page]]
   [playground.decimal.page :refer [decimal-page]]))


(defn playground-home-page
  [_req]
  (h/view
   {:render (fn [_ _req]
              [:motion.div.home-page
               (nav)
               [:h1.home-welcome "Welcome"]])}))

(def playground-routes
  [["/" {:name :home
         :title "Ant Man"
         :hyper/disabled? true
         :get (fn [req]
                (if (auth/signed-in? req)
                  {:status 302 :headers {"Location" "/trader/live"} :body ""}
                  {:status 302 :headers {"Location" "/login"} :body ""}))}]
   ["/playground"
    ["" {:name :playground/home
         :title "playground"
         :get playground-home-page}]
    ["/layout" {:name :playground/layout
                :title "Layout"
                :get #'layout-page}]
    ["/highcharts-random" {:name :playground/highcharts-random
                           :title "Highcharts random"
                           :get #'highcharts-random-page}]
    ["/simulator" {:name :playground/simulator
                   :title "Signal simulator"
                   :get #'simulator-page}]
    ["/notifications" {:name :playground/notifications
                       :title "Notifications"
                       :get #'notifications-page}]
    ["/winbox" {:name :playground/winbox
                :title "Winbox"
                :get #'winbox-page}]
    ["/decimal" {:name :playground/decimal
                 :title "Decimal input"
                 :get #'decimal-page}]]])


(defn routes [{:keys [token] :as ctx}]
  (concat playground-routes
          (token/routes token)
          (trader-routes/routes ctx)
          (admin-routes/routes ctx)))
