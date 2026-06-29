(ns quanta.blotter-hyper.admin.routes
  (:require
   [muuntaja.middleware]
   [ring.middleware.cookies]
   [ring.middleware.keyword-params]
   [ring.middleware.params]
   [token.identity.local]
   [webserver.middleware.ctx :as ctx]
   [quanta.blotter-hyper.auth :as auth]
   [quanta.blotter-hyper.admin.live :refer [live-page]]))

(defn- with-roles
  [required-roles route-data]
  (assoc route-data :render-middleware [(auth/wrap-require-roles required-roles)]))

(defn routes
  [ctx]
  (let [wrap-ctx (fn [handler]
                   (ctx/wrap-ctx handler ctx))
        wrap-identity (fn [handler]
                        (token.identity.local/wrap-identity handler (:token ctx)))]
    [["/admin/live" (with-roles #{:admin :trader}
                      {:name :admin-live
                       :title "Live"
                       :get #'live-page
                       :middleware [wrap-ctx
                                    wrap-identity]})]]))
