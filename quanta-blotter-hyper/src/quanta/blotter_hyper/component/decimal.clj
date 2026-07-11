(ns quanta.blotter-hyper.component.decimal
  (:require
   [hyper.core :as h]))

(declare Decimal Event)

(h/defc decimal-input
  "Client-side decimal editor. Click a digit to choose the step; arrow up/down
  adjust by that precision. Uses decimal.js for exact arithmetic.

  Size via host `:class` (`big-decimal-editor` / `small-decimal-editor`)
  and/or `:style` (CSS string) — these apply to the custom element host."
  {:require [["https://esm.sh/decimal.js@10.4.3" :as Decimal]]}
  [{:keys [value]}]

  (render
    (let [display (str (or value "0"))]
      [:div.decimal-editor
       [:div.decimal-highlight-layer display]
       [:input.decimal-input {:type "text" :spellcheck false :value display}]]))

  (mount [root]
    (let [input-el     (.querySelector root ".decimal-input")
          highlight-el (.querySelector root ".decimal-highlight-layer")
          Decimal-ctor (or (.-default Decimal) Decimal)
          digit-char?
          (fn [ch]
            (and ch (>= (.indexOf "0123456789" ch) 0)))
          digit-at-caret
          (fn [text caret]
            (let [len (.-length text)
                  ch  (when (< caret len) (.charAt text caret))]
              (cond
                (digit-char? ch) caret
                (and (> caret 0)
                     (digit-char? (.charAt text (dec caret)))) (dec caret)
                :else caret)))
          measure-char-width!
          (fn []
            (let [probe (.createElement js/document "span")
                  style (js/window.getComputedStyle input-el)]
              (set! (.-fontFamily (.-style probe)) (.-fontFamily style))
              (set! (.-fontSize (.-style probe)) (.-fontSize style))
              (set! (.-fontVariantNumeric (.-style probe)) "tabular-nums")
              (set! (.-textContent probe) "0")
              (set! (.-position (.-style probe)) "absolute")
              (set! (.-visibility (.-style probe)) "hidden")
              (.appendChild (.-body js/document) probe)
              (let [w (.-offsetWidth probe)]
                (.removeChild (.-body js/document) probe)
                w)))
          char-width* (atom nil)
          digit-from-click
          (fn [e]
            (let [text     (.-value input-el)
                  len      (.-length text)
                  style    (js/window.getComputedStyle input-el)
                  pad-left (+ (js/parseFloat (.-paddingLeft style))
                              (js/parseFloat (.-borderLeftWidth style)))
                  ch-w     (or @char-width* (reset! char-width* (measure-char-width!)))
                  idx      (js/Math.round (/ (- (.-offsetX e) pad-left) ch-w))]
              (max 0 (min (if (pos? len) (dec len) 0) idx))))
          step-for-digit
          (fn [text digit-idx]
            (let [dot         (.indexOf text ".")
                  decimal-pos (if (= dot -1) (.-length text) dot)]
              (cond
                (= (.charAt text digit-idx) "-") nil
                (< digit-idx decimal-pos)
                (js/Math.pow 10 (- decimal-pos digit-idx 1))
                (> digit-idx decimal-pos)
                (js/Math.pow 10 (- decimal-pos digit-idx))
                :else nil)))
          format-step
          (fn [step]
            (when step
              (.toString (Decimal-ctor. (str step)))))
          emit-state
          (fn [step]
            (emit "change"
                  {:value (.-value input-el)
                   :step  (format-step step)}))
          build-highlight
          (fn [text digit-idx]
            (loop [i 0 acc ""]
              (if (< i (.-length text))
                (let [ch (.charAt text i)
                      cls (if (= i digit-idx)
                            "decimal-char decimal-active"
                            "decimal-char")]
                  (recur (inc i)
                         (str acc
                              "<span class=\"" cls "\">" ch "</span>")))
                acc)))
          refresh-ui
          (fn []
            (let [text  (.-value input-el)
                  caret (or (.-selectionStart input-el) 0)
                  digit (digit-at-caret text caret)
                  step  (step-for-digit text digit)]
              (set! (.-innerHTML highlight-el) (build-highlight text digit))
              (emit-state step)))
          apply-step!
          (fn [dir]
            (let [text  (.-value input-el)
                  caret (or (.-selectionStart input-el) 0)
                  digit (digit-at-caret text caret)
                  step  (step-for-digit text digit)]
              (when step
                (let [current  (Decimal-ctor. (if (seq text) text "0"))
                      step-dec (Decimal-ctor. (str step))
                      next-val (if (= dir :up)
                                 (.plus current step-dec)
                                 (.minus current step-dec))
                      next-str (.toString next-val)]
                  (set! (.-value input-el) next-str)
                  (.setSelectionRange input-el digit digit)
                  (refresh-ui)))))]
      (set! (.-value input-el) (str (or value "0")))
      (refresh-ui)
      (doseq [ev ["input" "keyup" "select"]]
        (.addEventListener input-el ev refresh-ui))
      (.addEventListener
        input-el
        "mouseup"
        (fn [e]
          (let [digit (digit-from-click e)]
            (.setSelectionRange input-el digit digit)
            (refresh-ui))))
      (.addEventListener
        input-el
        "keydown"
        (fn [e]
          (when (#{"ArrowUp" "ArrowDown"} (.-key e))
            (.preventDefault e)
            (apply-step! (if (= "ArrowUp" (.-key e)) :up :down)))))))

  (update [root _old]
    (let [input-el (.querySelector root ".decimal-input")]
      (when (and (some? value) (not= (.-value input-el) (str value)))
        (set! (.-value input-el) (str value))
        (.dispatchEvent input-el (Event. "input"))))))
