(ns demo.routes
  (:require
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.token :as token]
   [quanta.blotter-hyper.trader.routes :as trader-routes]
   [quanta.blotter-hyper.admin.routes :as admin-routes]
   [antman.ui.highcharts-random-page :refer [highcharts-random-page]]
   [antman.ui.layout-page :refer [layout-page]]
   [antman.ui.simulator-page :refer [simulator-page]]))

(defn- with-roles
  [required-roles route-data]
  (assoc route-data :render-middleware [(auth/wrap-require-roles required-roles)]))

(def app-routes
  [["/" {:name :home
         :title "Ant Man"
         :hyper/disabled? true
         :get (fn [req]
                (if (auth/signed-in? req)
                  {:status 302 :headers {"Location" "/trader/live"} :body ""}
                  {:status 302 :headers {"Location" "/login"} :body ""}))}]
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
   ["/simulator"
    (with-roles #{:admin}
      {:name :simulator
       :title "Signal simulator"
       :get #'simulator-page})]])


(defn routes [{:keys [token] :as ctx}]
  (concat app-routes
          (token/routes token)
          (trader-routes/routes ctx)
          (admin-routes/routes ctx)))

