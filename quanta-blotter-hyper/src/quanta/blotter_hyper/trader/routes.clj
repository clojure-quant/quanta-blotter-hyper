(ns quanta.blotter-hyper.trader.routes
  (:require
   [muuntaja.middleware]
   [ring.middleware.cookies]
   [ring.middleware.keyword-params]
   [ring.middleware.params]
   [token.identity.local]
   [quanta.blotter-hyper.middleware :as middleware]
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.trader.accounts :refer [accounts-page]]
   [quanta.blotter-hyper.trader.backoffice :refer [backoffice-page]]
   [quanta.blotter-hyper.trader.home :refer [home-page]]
   [quanta.blotter-hyper.trader.live :refer [live-page]]
   [quanta.blotter-hyper.trader.quotes :refer [quotes-page]]))

(defn routes
  [ctx]
  (let [wrap-ctx (fn [handler]
                   (middleware/wrap-ctx handler ctx))
        wrap-identity  (fn [handler]
                         (token.identity.local/wrap-identity handler (:token ctx)))]
    [["/trader" {:middleware [wrap-ctx
                              wrap-identity
                              auth/wrap-hydrate-identity]
                 :render-middleware [(auth/wrap-require-roles #{:trader})]}

      ["" {:name :trader/home
           :title "Trader"
           :get #'home-page}]
      ["/backoffice" {:name :trader/backoffice
                      :title "Backoffice"
                      :get #'backoffice-page}]
      ["/accounts"  {:name :trader/accounts
                     :title "Accounts"
                     :get #'accounts-page}]
      ["/live" {:name :trader/live
                :title "Live"
                :get #'live-page}]
      ["/quotes"  {:name :trader/quotes
                   :title "Quotes"
                   :get #'quotes-page}]]]))

