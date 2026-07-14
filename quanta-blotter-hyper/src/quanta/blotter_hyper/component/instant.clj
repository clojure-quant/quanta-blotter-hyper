(ns quanta.blotter-hyper.component.instant
  "UTC instant view/edit. Date + time inputs interpret and emit UTC; clear sets nil."
  (:require
   [hyper.core :as h]
   [tick.core :as t])
  (:import
   (java.time Instant)
   (java.time.temporal ChronoUnit)))

(defn truncate-instant
  "Instant truncated to whole seconds."
  ^Instant [i]
  (.truncatedTo (t/instant i) ChronoUnit/SECONDS))

(defn format-instant-seconds
  "ISO-8601 UTC string truncated to whole seconds (no fractional part)."
  [i]
  (str (truncate-instant i)))

(h/defc instant-input
  "UTC instant editor with view/edit modes.

  View mode: readonly text showing the ISO-UTC string (or empty when nil),
  with clear (×) always next to it.
  Click the text → edit mode: grey chrome with date + time + × (no gaps).
  Pointer leaving the grey chrome returns to view mode.

  `:value` is a java.time.Instant (or tick instant), or nil.
  `:placeholder` is the empty-state hint (default \"nil\").
  Emits `change` with `{:value <iso-string-or-nil>}` (ISO seconds UTC);
  convert with `tick.core/instant` / `truncate-instant` on the server.

  Lifecycle forms are Squinting client code — use only JS (no JVM/tick)."
  [{:keys [value placeholder]}]

  (render
    [:div.instant-editor
     [:input.instant-view
      {:type "text"
       :readonly true
       :placeholder (or placeholder "nil")
       :spellcheck false
       :value ""}]
     ;; Chrome holds date/time (edit only) + clear (always). Grey bg in edit
     ;; mode; mouseleave on this box exits edit.
     [:div.instant-chrome
      [:div.instant-edit
       [:input.instant-date {:type "date" :value ""}]
       [:input.instant-time {:type "time" :step "1" :value ""}]]
      [:button.instant-clear {:type "button" :title "Clear"} "×"]]])

  (mount [root]
    (let [editor     (.querySelector root ".instant-editor")
          view-el    (.querySelector root ".instant-view")
          chrome-el  (.querySelector root ".instant-chrome")
          date-el    (.querySelector root ".instant-date")
          time-el    (.querySelector root ".instant-time")
          clear-el   (.querySelector root ".instant-clear")
          pad
          (fn [n]
            (let [s (str n)]
              (if (< n 10) (str "0" s) s)))
          ;; Always `YYYY-MM-DDTHH:MM:SSZ` — no millis/nanos.
          iso-from-date
          (fn [d]
            (str (.getUTCFullYear d) "-"
                 (pad (inc (.getUTCMonth d))) "-"
                 (pad (.getUTCDate d))
                 "T"
                 (pad (.getUTCHours d)) ":"
                 (pad (.getUTCMinutes d)) ":"
                 (pad (.getUTCSeconds d))
                 "Z"))
          iso-from-parts
          (fn []
            (let [date (.-value date-el)
                  time (.-value time-el)]
              (when (pos? (.-length date))
                (let [t0 (if (zero? (.-length time)) "00:00:00" time)
                      ;; Drop fractional seconds if a browser ever supplies them.
                      t1 (let [dot (.indexOf t0 ".")]
                           (if (>= dot 0) (.substring t0 0 dot) t0))
                      t* (if (= 5 (.-length t1)) (str t1 ":00") t1)]
                  (str date "T" t* "Z")))))
          set-view-text!
          (fn [iso]
            (set! (.-value view-el) (if (and iso (pos? (.-length (str iso))))
                                      (str iso)
                                      "")))
          apply-iso!
          (fn [v]
            (if (and v (pos? (.-length (str v))))
              (let [d (js/Date. (str v))]
                (if (js/isNaN (.getTime d))
                  (do (set! (.-value date-el) "")
                      (set! (.-value time-el) "")
                      (set-view-text! nil))
                  (let [iso (iso-from-date d)]
                    (set! (.-value date-el)
                          (str (.getUTCFullYear d) "-"
                               (pad (inc (.getUTCMonth d))) "-"
                               (pad (.getUTCDate d))))
                    (set! (.-value time-el)
                          (str (pad (.getUTCHours d)) ":"
                               (pad (.getUTCMinutes d)) ":"
                               (pad (.getUTCSeconds d))))
                    (set-view-text! iso))))
              (do (set! (.-value date-el) "")
                  (set! (.-value time-el) "")
                  (set-view-text! nil))))
          emit-current!
          (fn []
            (let [iso (iso-from-parts)]
              (set-view-text! iso)
              (emit "change" {:value iso})))
          enter-edit!
          (fn []
            (when-not (.-editing ctx)
              (.add (.-classList editor) "editing")
              (set! (.-editing ctx) true)
              (.focus date-el)))
          exit-edit!
          (fn []
            (when (.-editing ctx)
              (.blur date-el)
              (.blur time-el)
              (.remove (.-classList editor) "editing")
              (set! (.-editing ctx) false)
              (set-view-text! (iso-from-parts))))]
      (apply-iso! value)
      (set! (.-editing ctx) false)
      (.addEventListener
        view-el "click"
        (fn [_] (enter-edit!)))
      (.addEventListener date-el "input" (fn [_] (emit-current!)))
      (.addEventListener date-el "change" (fn [_] (emit-current!)))
      (.addEventListener time-el "input" (fn [_] (emit-current!)))
      (.addEventListener time-el "change" (fn [_] (emit-current!)))
      (.addEventListener
        date-el "click"
        (fn [_]
          (when (= "function" (js/typeof (.-showPicker date-el)))
            (.showPicker date-el))))
      (.addEventListener
        time-el "click"
        (fn [_]
          (when (= "function" (js/typeof (.-showPicker time-el)))
            (.showPicker time-el))))
      (.addEventListener
        clear-el "click"
        (fn [_]
          (set! (.-value date-el) "")
          (set! (.-value time-el) "")
          (set-view-text! nil)
          (emit "change" {:value nil})
          (exit-edit!)))
      ;; Leave edit mode when the pointer leaves the grey chrome (date/time/×).
      (.addEventListener
        chrome-el "mouseleave"
        (fn [_]
          (when (.-editing ctx)
            (exit-edit!))))
      (.addEventListener
        js/window "keydown"
        (fn [e]
          (when (and (.-editing ctx)
                     (= "Escape" (.-key e)))
            (.preventDefault e)
            (exit-edit!))))
      (set! (.-applyIso ctx) apply-iso!)))

  (update [root _old]
    (when-let [apply-iso! (.-applyIso ctx)]
      (when-not (.-editing ctx)
        (apply-iso! value)))))

;; Instant cannot cross the HTML attribute boundary. Coerce Instant/nil →
;; seconds ISO / nil so the client always sees a plain string (or absent).
(def instant-input
  (let [raw instant-input]
    (fn [attr-map & children]
      (apply raw
             (cond-> attr-map
               (contains? attr-map :value)
               (assoc :value (some-> (:value attr-map) format-instant-seconds)))
             children))))

;;; ---------------------------------------------------------------------------
;;; Relative UTC presets (computed at apply time on the server)

(defn yesterday-midnight-utc
  ^Instant []
  (let [today-utc (-> (t/instant) (t/in t/UTC) t/date)
        yday (t/<< today-utc (t/new-period 1 :days))]
    (truncate-instant (t/instant (t/in (t/at yday (t/new-time 0 0 0)) t/UTC)))))

(defn yesterday-same-time-utc
  ^Instant []
  (truncate-instant (t/<< (t/instant) (t/new-duration 1 :days))))

(defn seven-days-ago-utc
  ^Instant []
  (truncate-instant (t/<< (t/instant) (t/new-duration 7 :days))))

(def preset-options
  [["yesterday-midnight" "yesterday midnight"]
   ["yesterday-same" "yesterday same time"]
   ["7-days-ago" "7 days ago"]])

(defn apply-preset
  ^Instant [key]
  (case key
    "yesterday-midnight" (yesterday-midnight-utc)
    "yesterday-same" (yesterday-same-time-utc)
    "7-days-ago" (seven-days-ago-utc)
    nil))

;;; ---------------------------------------------------------------------------
;;; Shortcut UI wrapper (compose instant-input)

(defn- cursor->instant
  "Return Instant or nil. Treat legacy \"\" cleared sentinel as nil."
  [v]
  (when (and (some? v) (not= "" v))
    (truncate-instant v)))

(defn- change->instant
  "Client emits ISO string or nil → Instant or nil."
  [detail]
  (some-> (:value detail) truncate-instant))

(defn- bind-instant
  [state* k opts]
  (instant-input
   (merge
    {:value (cursor->instant (get @state* k))
     :data-on:change (h/action (swap! state* assoc k (change->instant $detail)))}
    opts)))

(defn instant-with-menu
  "Presets details/summary menu + instant-input bound to `(get @state* k)`.
  `state*` holds a map; `k` is Instant or nil."
  [state* k & [opts]]
  [:div.instant-with-menu
   [:details.instant-preset-menu
    [:summary "Presets…"]
    [:div.instant-preset-menu-body
     (for [[pk label] preset-options]
       [:button.instant-preset-btn
        {:type "button"
         :data-on:click (h/action (swap! state* assoc k (apply-preset pk)))}
        label])]]
   (bind-instant state* k opts)])
