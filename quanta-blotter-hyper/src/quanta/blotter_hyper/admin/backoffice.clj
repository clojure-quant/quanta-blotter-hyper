(ns quanta.blotter-hyper.admin.backoffice
  (:require
   [quanta.blotter-hyper.nav :as nav]
   [quanta.blotter-hyper.trader.backoffice :as backoffice]))

(def backoffice-page
  (fn [req] (backoffice/backoffice-page nav/admin-nav req {:trader nil})))
