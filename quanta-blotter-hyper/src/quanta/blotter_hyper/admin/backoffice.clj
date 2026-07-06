(ns quanta.blotter-hyper.admin.backoffice
  (:require
   [quanta.blotter-hyper.trader.backoffice :as backoffice]))

(def backoffice-page
  (fn [{:keys [hyper/env] :as req}]
    (backoffice/backoffice-page (:admin/nav env) req {:trader nil})))
