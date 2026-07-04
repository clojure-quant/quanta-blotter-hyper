(ns playground.winbox.component
  (:require
   [hyper.core :as h]))

(h/defc winbox-window
  "Client-side web component that opens a WinBox window and renders props-driven
  hiccup into its body."
  [{:keys [id title width height x y message]}]

  (render
    [:div.winbox-body
     [:h2 title]
     [:p message]])

  (mount [root]
    (let [closed-by-winbox* (atom false)
          onclose           (fn []
                              (reset! closed-by-winbox* true)
                              (emit "closed" {:id id})
                              false)
          opts              (js/Object.)]
      (set! (.-title opts) title)
      (set! (.-width opts) width)
      (set! (.-height opts) height)
      (set! (.-x opts) (or x "center"))
      (set! (.-y opts) (or y "center"))
      (set! (.-onclose opts) onclose)
      (let [wb (js/WinBox. opts)]
        (set! (.-winbox ctx) wb)
        (let [body (.-body wb)]
          (while (.-firstChild root)
            (.appendChild body (.removeChild root (.-firstChild root))))))))

  (unmount [_root]
    (when-let [wb (.-winbox ctx)]
      (.close wb true))))
