(ns quanta.blotter-hyper.nav
  (:require
   [hyper.core :as h]))

(defn blotter-links
  []
  [[:a (h/navigate :backoffice) "Backoffice"]
   " · "
   [:a (h/navigate :accounts) "Accounts"]
   " · "
   [:a (h/navigate :trader-live) "Live"]
   " · "
   [:a (h/navigate :admin-live) "Admin live"]])

(defn nav
  []
  (into [:nav.app-nav] (blotter-links)))
