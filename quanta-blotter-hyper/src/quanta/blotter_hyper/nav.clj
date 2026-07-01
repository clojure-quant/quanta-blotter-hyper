(ns quanta.blotter-hyper.nav
  (:require
   [hyper.core :as h]))

(defn- current-user-name []
  (some-> @(h/session-cursor :identity) :user name))

(defn trader-nav
  []
  [:nav.app-nav.blotter-nav
   [:div.blotter-nav-links
    [:a (h/navigate :trader/live) "Live"]
    " · "
    [:a (h/navigate :trader/quotes) "Quotes"]
    " · "
    [:a (h/navigate :trader/backoffice) "Backoffice"]
    " · "
    [:a (h/navigate :trader/accounts) "Accounts"]]
   [:span.blotter-nav-label (str "Trader: " (current-user-name))]])

(defn admin-nav
  []
  [:nav.app-nav.blotter-nav
   [:div.blotter-nav-links
    [:a (h/navigate :admin/live) "Live"]
    " · "
    [:a (h/navigate :admin/backoffice) "Backoffice"]
    " · "
    [:a (h/navigate :admin/accounts) "Accounts"]
    " · "
    [:a (h/navigate :admin/assets) "Assets"]]
   [:span.blotter-nav-label (str "Admin: " (current-user-name))]])
