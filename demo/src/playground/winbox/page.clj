(ns playground.winbox.page
  (:require
   [hyper.core :as h]
   [playground.nav :refer [nav]]
   [playground.winbox.component :refer [winbox-window]]))

(defn- open-window! [windows* message]
  (swap! windows* conj {:id      (str (random-uuid))
                        :title   "Demo Window"
                        :width   400
                        :height  300
                        :message message}))

(defn- close-window! [windows* detail]
  (let [id (:id detail)]
    (swap! windows* #(vec (remove (fn [w] (= (:id w) id)) %)))))

(defn winbox-page
  [_req]
  (let [windows* (h/tab-cursor :open-windows [])]
    [:motion.div.winbox-page
     (nav)
     [:h1 "Winbox"]
     [:button.winbox-open-btn
      {:type "button"
       :data-on:click (h/action (open-window! windows* "Hello from WinBox!"))}
      "Open window 1"]
     [:button.winbox-open-btn
      {:type "button"
       :data-on:click (h/action (open-window! windows* "Welcome to Panama!"))}
      "Open window 2"]
     (for [w @windows*]
       (winbox-window
         (assoc w
                :style "display:none"
                :data-on:closed
                (h/action (close-window! windows* $detail)))))]))
