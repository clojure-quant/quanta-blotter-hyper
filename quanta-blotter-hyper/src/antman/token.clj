(ns antman.token
  (:require
   [muuntaja.middleware]
   [ring.middleware.cookies]
   [ring.middleware.keyword-params]
   [ring.middleware.params]
   [token.identity.local]
   [token.identity.page.login]
   [token.identity.page.me]
   [token.oauth2.handler.authresult]
   [token.oauth2.handler.redirect2]
   [token.oauth2.handler.start]
   [webserver.middleware.ctx :as ctx]))

(defn- wrap-token-ctx [token]
  (fn [handler]
    (ctx/wrap-ctx handler {:token token})))

(defn- wrap-token-identity [token]
  (fn [handler]
    (token.identity.local/wrap-identity handler token)))

(defn routes
  "Ring/reitit routes for identity and OAuth2. Merged into Hyper's router;
   :hyper/disabled? prevents Hyper from wrapping handlers as SPA pages."
  [token]
  (let [wrap-ctx (wrap-token-ctx token)
        wrap-identity (wrap-token-identity token)]
    [["/login" {:hyper/disabled? true
                :get token.identity.page.login/login-page
                :middleware [ring.middleware.params/wrap-params]}]
     ["/me" {:hyper/disabled? true
             :get token.identity.page.me/me-page
             :middleware [wrap-ctx
                          ring.middleware.cookies/wrap-cookies
                          muuntaja.middleware/wrap-format
                          wrap-identity]}]
     ["/token"
      ["/login" {:post token.identity.local/login-handler
                 :middleware [wrap-ctx
                              ring.middleware.cookies/wrap-cookies
                              ring.middleware.params/wrap-params
                              muuntaja.middleware/wrap-format]}]
      ["/logout" {:get token.identity.local/logout-handler
                  :middleware [ring.middleware.cookies/wrap-cookies]}]
      ["/me" {:get token.identity.local/me-handler
              :middleware [wrap-ctx
                           ring.middleware.cookies/wrap-cookies
                           muuntaja.middleware/wrap-format
                           wrap-identity]}]
      ["/oauth2"
       ["/start/:provider" {:get token.oauth2.handler.start/handler-oauth2-start
                            :middleware [wrap-ctx
                                         muuntaja.middleware/wrap-format
                                         ring.middleware.params/wrap-params
                                         ring.middleware.keyword-params/wrap-keyword-params]}]
       ["/redirect/:provider" {:handler token.oauth2.handler.redirect2/handler-oauth2-redirect
                               :middleware [wrap-ctx
                                            ring.middleware.cookies/wrap-cookies
                                            ring.middleware.params/wrap-params
                                            ring.middleware.keyword-params/wrap-keyword-params
                                            muuntaja.middleware/wrap-format]}]
       ["/authresult" {:hyper/disabled? true
                       :get token.oauth2.handler.authresult/page-authresult
                       :middleware [ring.middleware.params/wrap-params]}]]]]))
