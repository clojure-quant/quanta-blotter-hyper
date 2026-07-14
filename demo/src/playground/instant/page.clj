(ns playground.instant.page
  (:require
   [hyper.core :as h]
   [tick.core :as t]
   [playground.nav :refer [nav]]
   [quanta.blotter-hyper.component.instant :as instant]))

(defn- fmt-value
  [v]
  (if (nil? v) "nil" (str v)))

(defn- on-instant-change
  "Parse client ISO emission to Instant or nil."
  [state* k]
  (h/action
   (swap! state* assoc k (some-> (:value $detail) instant/truncate-instant))))

(defn- instant-input-demo
  "One demo row that uses only `instant-input` (no presets wrapper)."
  [placeholder state* k]
  [:div.instant-row
   [:p.instant-row-label placeholder]
   (instant/instant-input
    {:value (get @state* k)
     :placeholder placeholder
     :data-on:change (on-instant-change state* k)})
   [:p.instant-display
    "Value: "
    [:span.instant-display-value (fmt-value (get @state* k))]]])

(defn instant-page
  [_req]
  ;; One map cursor so Instant keys can be nil without tab-cursor's
  ;; (cas nil default) re-initializing them on every render.
  (let [state* (h/tab-cursor :instant-page
                             {:now (instant/truncate-instant (t/instant))
                              :nil nil
                              :fixed (instant/truncate-instant "2020-05-27T18:07:07Z")
                              :menu (instant/truncate-instant (t/instant))})]
    [:motion.div.instant-page
     (nav)
     [:h1 "Instant input (UTC)"]
     [:p.decimal-hint
      "Date and time are always UTC. Clear (×) sets the value to nil."]

     [:section.instant-section
      [:h2 "instant-input only"]
      (instant-input-demo "start time" state* :now)
      (instant-input-demo "end time" state* :nil )
      (instant-input-demo "emergency meeting time" state* :fixed)]

     [:section.instant-section
      [:h2 "instant-with-menu"]
      (instant/instant-with-menu state* :menu {:placeholder "preset or edit…"})
      [:p.instant-display
       "Value: "
       [:span.instant-display-value (fmt-value (:menu @state*))]]]]))
