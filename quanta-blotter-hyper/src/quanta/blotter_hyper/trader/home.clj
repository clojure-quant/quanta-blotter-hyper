(ns quanta.blotter-hyper.trader.home
  (:require
   [hyper.core :as h]))

(defn home-page
  [{:keys [hyper/env] :as _req}]
  (h/view
   {:render (fn [_ _req]
              (let [user (some-> @(h/session-cursor :identity) :user name)]
                [:motion.div.home-page
                 ((:trader/nav env))
                 [:h1.home-welcome (str "Welcome Trader " user)]]))}))
