(ns quanta.blotter-hyper.handler
  (:require
   [hyper.core :as h]
   [quanta.blotter-hyper.auth :as auth]
   ))

(defn head-tags
  [_req]
  ;; Scripts/CSS for layout and highcharts-random live in the base head so
  ;; Hyper client-side navigation between pages still has them loaded.
  [[:link {:rel "stylesheet" :href "/css/app.css"}]
   [:link {:rel "stylesheet"
           :href "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/dist/css/goldenlayout-base.css"}]
   [:link {:rel "stylesheet"
           :href "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/dist/css/themes/goldenlayout-dark-theme.css"}]
   [:script {:type "module" :src "/js/golden-layout.js?v=4"}]
   [:script {:type "module" :src "/js/highcharts-random.js?v=4"}]])

(defn create-handler
  "Ring handler for Hyper.

  Optional `extra-watches` are merged with the default SSE watches.
  Pass atoms via `(deref ns/var)` in services.edn."
  ([routes]
   (create-handler routes nil))
  ([routes extra-watches]
   (h/create-handler
    routes
    :static-resources "public"
    :head #'head-tags
    :watches (into [] (or extra-watches [])))))
