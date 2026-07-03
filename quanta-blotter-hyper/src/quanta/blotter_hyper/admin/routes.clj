(ns quanta.blotter-hyper.admin.routes
  (:require
   [muuntaja.middleware]
   [ring.middleware.cookies]
   [ring.middleware.keyword-params]
   [ring.middleware.params]
   [token.identity.local]
   [quanta.blotter-hyper.middleware :as middleware]
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.admin.accounts :refer [accounts-page]]
   [quanta.blotter-hyper.admin.assets :refer [assets-page]]
   [quanta.blotter-hyper.admin.backoffice :refer [backoffice-page]]
   [quanta.blotter-hyper.admin.home :refer [home-page]]
   [quanta.blotter-hyper.admin.live :refer [live-page]]))


(defn routes
  [ctx]
  (let [wrap-ctx (fn [handler]
                   (middleware/wrap-ctx handler ctx))
        wrap-identity (fn [handler]
                        (token.identity.local/wrap-identity handler (:token ctx)))]
    [["/admin" {:middleware [wrap-ctx
                             wrap-identity
                             auth/wrap-hydrate-identity]
                :render-middleware [(auth/wrap-require-roles #{:admin})]}

      [""  {:name :admin/home
            :title "Admin"
            :get #'home-page}]
      ["/live" {:name :admin/live
                :title "Live"
                :get #'live-page}]
      ["/backoffice"  {:name :admin/backoffice
                       :title "Backoffice"
                       :get #'backoffice-page}]
      ["/accounts"  {:name :admin/accounts
                     :title "Accounts"
                     :get #'accounts-page}]
      ["/assets"  {:name :admin/assets
                   :title "Assets"
                   :get #'assets-page}]]]))
