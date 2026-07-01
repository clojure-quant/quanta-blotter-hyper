(ns quanta.blotter-hyper.trader.routes
  (:require
   [muuntaja.middleware]
   [ring.middleware.cookies]
   [ring.middleware.keyword-params]
   [ring.middleware.params]
   [token.identity.local]
   [webserver.middleware.ctx :as ctx]
    [quanta.blotter-hyper.middleware :as middleware]
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.trader.accounts :refer [accounts-page]]
   [quanta.blotter-hyper.trader.backoffice :refer [backoffice-page]]
   [quanta.blotter-hyper.trader.home :refer [home-page]]
   [quanta.blotter-hyper.trader.live :refer [live-page]]
   [quanta.blotter-hyper.trader.quotes :refer [quotes-page]]))


(defn- with-roles
  [required-roles route-data]
  (assoc route-data :render-middleware [(auth/wrap-require-roles required-roles)]))


(defn routes
  [ctx]
  (let [wrap-ctx (fn [handler]
                   (middleware/wrap-ctx handler ctx))
        wrap-identity  (fn [handler]
                         (token.identity.local/wrap-identity handler (:token ctx)))]
    [["/trader" (with-roles #{:trader}
                  {:name :trader/home
                   :title "Trader"
                   :get #'home-page
                   :middleware [wrap-ctx
                                wrap-identity]})]
     ["/trader/backoffice" {:name :trader/backoffice
                            :title "Backoffice"
                            :get #'backoffice-page
                            :middleware [wrap-ctx
                                         wrap-identity]}]
     ["/trader/accounts" (with-roles #{:trader}
                           {:name :trader/accounts
                            :title "Accounts"
                            :get #'accounts-page
                            :middleware [wrap-ctx
                                         wrap-identity]})]
     ["/trader/live" (with-roles #{:trader}
                       {:name :trader/live
                        :title "Live"
                        :get #'live-page
                        :middleware [wrap-ctx
                                     wrap-identity]})]
     ["/trader/quotes" (with-roles #{:trader}
                         {:name :trader/quotes
                          :title "Quotes"
                          :get #'quotes-page
                          :middleware [wrap-ctx
                                       wrap-identity]})]]))

