(ns demo.head)

(defn head-tags
  [_req]
  ;; Scripts/CSS for layout and highcharts-random live in the base head so
  ;; Hyper client-side navigation between pages still has them loaded.
  [[:link {:rel "stylesheet" :href "/css/blotter-hyper.css"}]
   [:link {:rel "stylesheet" :href "/css/playground.css"}]
   [:link {:rel "stylesheet"
           :href "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/dist/css/goldenlayout-base.css"}]
   [:link {:rel "stylesheet"
           :href "https://cdn.jsdelivr.net/npm/golden-layout@2.6.0/dist/css/themes/goldenlayout-dark-theme.css"}]
   [:script {:type "module" :src "/js/golden-layout.js?v=4"}]
   [:script {:type "module" :src "/js/highcharts-random.js?v=4"}]
   [:script {:src "https://cdn.jsdelivr.net/npm/winbox@0.2.82/dist/winbox.bundle.min.js"}]
   [:script {:src "/js/beep.js" :defer true}]])
