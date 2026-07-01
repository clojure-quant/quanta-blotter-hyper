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

(defn- with-roles
  [required-roles route-data]
  (assoc route-data :render-middleware [(auth/wrap-require-roles required-roles)]))

(defn routes
  [ctx]
  (let [wrap-ctx (fn [handler]
                   (middleware/wrap-ctx handler ctx))
        wrap-identity (fn [handler]
                        (token.identity.local/wrap-identity handler (:token ctx)))]
    [["/admin" (with-roles #{:admin}
                 {:name :admin/home
                  :title "Admin"
                  :get #'home-page
                  :middleware [wrap-ctx
                               wrap-identity]})]
     ["/admin/live" (with-roles #{:admin}
                      {:name :admin/live
                       :title "Live"
                       :get #'live-page
                       :middleware [wrap-ctx
                                    wrap-identity]})]
     ["/admin/backoffice" (with-roles #{:admin}
                            {:name :admin/backoffice
                             :title "Backoffice"
                             :get #'backoffice-page
                             :middleware [wrap-ctx
                                          wrap-identity]})]
     ["/admin/accounts" (with-roles #{:admin}
                          {:name :admin/accounts
                           :title "Accounts"
                           :get #'accounts-page
                           :middleware [wrap-ctx
                                        wrap-identity]})]
     ["/admin/assets" (with-roles #{:admin}
                        {:name :admin/assets
                         :title "Assets"
                         :get #'assets-page
                         :middleware [wrap-ctx
                                      wrap-identity]})]]))
