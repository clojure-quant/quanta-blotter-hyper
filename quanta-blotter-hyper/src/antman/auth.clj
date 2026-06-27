(ns antman.auth
  (:require
   [hyper.core :as h]
   [token.core :as token]
   [token.identity.local :as local-identity]))

(defonce token* (atom nil))

(defn start-token-service!
  [{:keys [users token]}]
  (let [token-cfg {:users users
                   :secret (:secret token)
                   :store-path (:store-path token)
                   :providers (:oauth2 token)
                   :auth-expiry (:auth-expiry token)}
        svc (token/start-token-service token-cfg)]
    (reset! token* svc)
    svc))

(defn normalize-roles [roles]
  (into #{} (map keyword) (or roles #{})))

(defn read-identity
  [req]
  (when-let [token @token*]
    (when-let [cookie (get-in req [:cookies "identity" :value])]
      (let [claim (local-identity/verify-token token cookie)]
        (when (:user claim)
          {:user (keyword (:user claim))
           :roles (normalize-roles (:roles claim))
           :email (:email claim)
           :provider (:provider claim)})))))

(defn signed-in?
  [req]
  (some? (:user (read-identity req))))

(defn role-authorized?
  "Roles are separate; user must hold at least one of required-roles."
  [user-roles required-roles]
  (boolean (some required-roles user-roles)))

(defn wrap-hydrate-identity
  "Ring middleware: verify identity cookie and hydrate session :identity cursor."
  [handler]
  (fn [req]
    (let [app-state* (:hyper/app-state req)
          session-id (:hyper/session-id req)
          identity (read-identity req)]
      (when (and app-state* session-id identity)
        (swap! app-state* assoc-in [:sessions session-id :data :identity] identity))
      (handler req))))

(defn wrap-require-roles
  "Render middleware: redirect to /login or deny when roles are insufficient."
  [required-roles]
  (fn [handler]
    (fn [req]
      (let [identity @(h/session-cursor :identity)
            user-roles (:roles identity)]
        (cond
          (nil? (:user identity))
          {:status 302 :headers {"Location" "/login"} :body ""}

          (not (role-authorized? user-roles required-roles))
          [:div.page
           [:h1 "Access denied"]
           [:p "You do not have permission to view this page."]
           [:p "Signed in as " (str (:user identity))
            " with roles " (pr-str (sort user-roles)) "."]]

          :else
          (handler req))))))
