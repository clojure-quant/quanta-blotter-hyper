(ns playground.decimal.page
  (:require
   [hyper.core :as h]
   [playground.nav :refer [nav]]
   [playground.decimal.component :refer [decimal-input]]))

(defn decimal-page
  [_req]
  (let [value* (h/tab-cursor :decimal-value "123.456")
        step*  (h/tab-cursor :decimal-step nil)]
    [:motion.div.decimal-page
     (nav)
     [:h1 "Decimal input"]
     [:p.decimal-hint
      "Click a digit to set the step. Arrow up increases, arrow down decreases."]
     (decimal-input
       {:value @value*
        :data-on:change
        (h/action
          (reset! value* (:value $detail))
          (reset! step* (:step $detail)))})
     [:p.decimal-display
      "Value: "
      [:span.decimal-display-value (str @value*)]]
     [:p.decimal-display
      "Resolution: "
      [:span.decimal-display-step (or @step* "—")]]]))
