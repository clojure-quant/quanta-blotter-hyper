(ns quanta.blotter-hyper.admin.home
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.nav :as nav]))

(defn home-page
  [_req]
  (h/view
   {:render (fn [_ _req]
              (let [user (some-> @(h/session-cursor :identity) :user name)]
                [:motion.div.home-page
                 (nav/admin-nav)
                 [:h1.home-welcome (str "Welcome Admin " user)]]))}))
