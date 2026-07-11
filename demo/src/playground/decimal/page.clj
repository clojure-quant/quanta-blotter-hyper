(ns playground.decimal.page
  (:require
   [hyper.core :as h]
   [playground.nav :refer [nav]]
   [quanta.blotter-hyper.component.decimal :refer [decimal-input]]))

(defn- editor
  [value* step* opts]
  (decimal-input
    (merge
      {:value @value*
       :data-on:change
       (h/action
         (reset! value* (:value $detail))
         (reset! step* (:step $detail)))}
      opts)))

(defn decimal-page
  [_req]
  (let [value* (h/tab-cursor :decimal-value "123.456")
        step*  (h/tab-cursor :decimal-step nil)]
    [:motion.div.decimal-page
     (nav)
     [:h1 "Decimal input"]
     [:p.decimal-hint
      "Click a digit to set the step. Arrow up increases, arrow down decreases."]
     [:div.decimal-size-row
      [:p.decimal-size-label "big-decimal-editor"]
      (editor value* step* {:class "big-decimal-editor"})]
     [:div.decimal-size-row
      [:p.decimal-size-label "small-decimal-editor"]
      (editor value* step* {:class "small-decimal-editor"})]
     [:div.decimal-size-row
      [:p.decimal-size-label "style: medium"]
      (editor value* step*
              {:style "font-size: 1rem; line-height: 1.35; min-width: 9rem;"})]
     [:div.decimal-size-row
      [:p.decimal-size-label "style: x-large"]
      (editor value* step*
              {:style "font-size: 1.75rem; line-height: 1.6; min-width: 16rem;"})]
     [:p.decimal-display
      "Value: "
      [:span.decimal-display-value (str @value*)]]
     [:p.decimal-display
      "Resolution: "
      [:span.decimal-display-step (or @step* "—")]]]))
