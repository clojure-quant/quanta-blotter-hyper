(ns antman.ui.highcharts-random-page
  (:require
   [antman.ui.components :as ui]))

(defn highcharts-random-page
  [_req]
  [:motion.div.highcharts-random-page
   (ui/nav)
   [:h1 "Highcharts random"]
   [:p.muted
    "12 scrolling line charts · 180 points each · +1 price tick per second per chart."]
   [:motion.div#charts-grid.charts-grid
    {:data-on-load "antmanInitHighchartsRandom()"}]])
