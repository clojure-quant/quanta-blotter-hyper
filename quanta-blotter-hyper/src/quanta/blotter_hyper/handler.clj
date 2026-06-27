(ns quanta.blotter-hyper.handler)

(defn head-tags
  [_req]
  ;; Scripts/CSS for layout and highcharts-random live in the base head so
  ;; Hyper client-side navigation between pages still has them loaded.
  [[:link {:rel "stylesheet" :href "/css/app.css"}]
   [:script {:src "/js/sse-reconnect.js" :defer true}]
   [:link {:rel "stylesheet"
           :href "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/dist/css/goldenlayout-base.css"}]
   [:link {:rel "stylesheet"
           :href "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/dist/css/themes/goldenlayout-dark-theme.css"}]
   [:script {:type "module" :src "/js/golden-layout.js?v=4"}]
   [:script {:type "module" :src "/js/highcharts-random.js?v=4"}]])