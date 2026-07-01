(ns quanta.blotter-hyper.trader.quotes
  (:require
   [hyper.core :as h]
   [quanta.asset.datahike :refer [list-names]]
   [quanta.blotter-hyper.nav :as nav]
   [quanta.blotter-hyper.view.quotes :as quotes-view]
   [quanta.quote.core :refer [create-quotelist-consumer]]))

(defn- quotes-header [state-a list-a prev-a flash-quotes-a]
  (let [{:keys [lists]} @state-a
        current-list @list-a]
    [:header.quotes-header
     [:h1 "Quotes"]
     [:select {:value current-list
               :data-on:change
               (h/action
                (let [list-name $value]
                  (reset! list-a list-name)
                  (reset! prev-a {})
                  (reset! flash-quotes-a {})
                  (swap! state-a assoc :list list-name)))}
      (for [name lists]
        [:option {:key name :value name :selected (= name current-list)} name])]]))

(defn quotes-page
  [{:keys [hyper/env] :as _req}]
  (h/view
   {:mount (fn []
             (let [db (:db env)
                   qm (:quote-manager env)
                   _ (assert db ":db needs to be in :ctx")
                   _ (assert qm ":quote-manager needs to be in :ctx")
                   lists (list-names db)
                   list-a (atom "default")
                   state-a (atom {:lists lists :list "default"})
                   prev-a (atom {})
                   flash-quotes-a (atom {})
                   {:keys [quotelist dispose!]}
                   (create-quotelist-consumer qm list-a)
                   _ (add-watch quotelist :flash
                                (fn [_ _ _ quotes]
                                  (let [[enriched next-prev]
                                        (quotes-view/enrich-quotes-with-flash quotes @prev-a)]
                                    (reset! prev-a next-prev)
                                    (reset! flash-quotes-a enriched))))]
               (h/watch! state-a)
               (h/watch! flash-quotes-a)
               {:state-a state-a
                :list-a list-a
                :prev-a prev-a
                :flash-quotes-a flash-quotes-a
                :quotelist quotelist
                :dispose! dispose!}))
    :render (fn [{:keys [state-a list-a prev-a flash-quotes-a]} _req]
              [:motion.div.quotes-page
               (nav/trader-nav)
               [:div.quotes-layout
                (quotes-header state-a list-a prev-a flash-quotes-a)
                [:div.quotes-main
                 (quotes-view/quotes-table @flash-quotes-a)]]])
    :unmount (fn [{:keys [dispose! quotelist]}]
               (remove-watch quotelist :flash)
               (dispose!))}))
