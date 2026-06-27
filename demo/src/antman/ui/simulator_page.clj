(ns antman.ui.simulator-page
  (:require
   [cheshire.core :as json]
   [clojure.string :as str]
   [hyper.core :as h]
   [antman.simulator.signal :as sim-signal]
   [antman.ui.components :as ui]))

(defn- sig [trade path]
  (h/signal path (sim-signal/field-default trade path)))

(defn- field
  ([label sig] (field label sig {}))
  ([label sig {:keys [type span rows default]}]
   [:label.simulator-field
    {:class (not-empty (str/join " "
                                 (remove nil?
                                         [(when span (str "simulator-span-" span))
                                          (when (= type :check) "simulator-check")
                                          (when (= type :area) "simulator-area")])))}
    [:span.simulator-label label]
    (case type
      :num [:input {:type "number" :step "any" :data-bind sig :value default}]
      :check [:input {:type "checkbox" :data-bind sig :checked default}]
      :area [:textarea {:rows (or rows 2) :data-bind sig}]
      [:input {:type "text" :data-bind sig}])]))

(defn- tp-field-key [n field]
  (keyword (str "tp-" n "-" (name field))))

(defn- take-profit-signals [trade n]
  (let [tp (get-in trade [:take-profits n])]
    {:level (h/signal (tp-field-key n :level) (:level tp))
     :price (h/signal (tp-field-key n :price) (:price tp))
     :percent (h/signal (tp-field-key n :percent) (:percent tp))
     :hit (h/signal (tp-field-key n :hit) (:hit tp))
     :defaults tp}))

(defn- take-profit-row [n {:keys [level price percent hit defaults]}]
  [:tr {:key n}
   [:td (str "TP" (inc n))]
   [:td [:input {:type "number" :step "1" :data-bind level :value (:level defaults)}]]
   [:td [:input {:type "number" :step "any" :data-bind price :value (:price defaults)}]]
   [:td [:input {:type "number" :step "any" :data-bind percent :value (:percent defaults)}]]
   [:td [:input {:type "checkbox" :data-bind hit :checked (:hit defaults)}]]])

(defn- submit-signal! [triggered* tp-signals & signals]
  (let [[trade-id* lifecycle-id* symbol* direction* status* source-protocol*
         entry-order-type* requested-entry-price* filled-price* initial-stop-price*
         current-stop-price* quantity* remaining-quantity* remaining-pct* net-r*
         opened-at* updated-at* closed-at* close-reason* broker-ref*
         broker-position-id* rd-outcome* rd-profile-name* rd-violations*]
        signals
        signal (sim-signal/assemble-trade
                {[:trade-id] @trade-id*
                 [:lifecycle-id] @lifecycle-id*
                 [:symbol] @symbol*
                 [:direction] @direction*
                 [:status] @status*
                 [:source-protocol] @source-protocol*
                 [:entry-order-type] @entry-order-type*
                 [:requested-entry-price] @requested-entry-price*
                 [:filled-price] @filled-price*
                 [:initial-stop-price] @initial-stop-price*
                 [:current-stop-price] @current-stop-price*
                 [:take-profits]
                 (vec (for [{:keys [level price percent hit]} tp-signals]
                        {:level @level
                         :price @price
                         :percent @percent
                         :hit @hit}))
                 [:quantity] @quantity*
                 [:remaining-quantity] @remaining-quantity*
                 [:remaining-pct] @remaining-pct*
                 [:net-r] @net-r*
                 [:opened-at] @opened-at*
                 [:updated-at] @updated-at*
                 [:closed-at] @closed-at*
                 [:close-reason] @close-reason*
                 [:broker-ref] @broker-ref*
                 [:broker-position-id] @broker-position-id*
                 [:risk-decision :outcome] @rd-outcome*
                 [:risk-decision :profile-name] @rd-profile-name*
                 [:risk-decision :violations] @rd-violations*})]
    (println "Received signal:" (pr-str signal))
    (reset! triggered* true)
    (future
      (Thread/sleep 5000)
      (reset! triggered* false))))

(defn simulator-page
  [_req]
  (let [trade (sim-signal/load-signal)
        trade-id* (sig trade [:trade-id])
        lifecycle-id* (sig trade [:lifecycle-id])
        symbol* (sig trade [:symbol])
        direction* (sig trade [:direction])
        status* (sig trade [:status])
        source-protocol* (sig trade [:source-protocol])
        entry-order-type* (sig trade [:entry-order-type])
        requested-entry-price* (sig trade [:requested-entry-price])
        filled-price* (sig trade [:filled-price])
        initial-stop-price* (sig trade [:initial-stop-price])
        current-stop-price* (sig trade [:current-stop-price])
        quantity* (sig trade [:quantity])
        remaining-quantity* (sig trade [:remaining-quantity])
        remaining-pct* (sig trade [:remaining-pct])
        net-r* (sig trade [:net-r])
        opened-at* (sig trade [:opened-at])
        updated-at* (sig trade [:updated-at])
        closed-at* (sig trade [:closed-at])
        close-reason* (sig trade [:close-reason])
        broker-ref* (sig trade [:broker-ref])
        broker-position-id* (sig trade [:broker-position-id])
        rd-outcome* (sig trade [:risk-decision :outcome])
        rd-profile-name* (sig trade [:risk-decision :profile-name])
        rd-violations* (h/signal [:risk-decision :violations]
                                 (json/generate-string (:violations (:risk-decision trade))))
        submit-triggered* (h/signal :submit-triggered false)
        tp-signals (vec (for [n (range (count (:take-profits trade)))]
                          (take-profit-signals trade n)))]
    [:motion.div.simulator-page
     (ui/nav)
     [:h1 "Signal simulator"]
     [:div.simulator-form
      [:div.simulator-grid
       (field "ID" trade-id*)
       (field "Lifecycle" lifecycle-id*)
       (field "Symbol" symbol*)
       (field "Dir" direction*)
       (field "Status" status*)
       (field "Protocol" source-protocol*)
       (field "Entry type" entry-order-type*)
       (field "Req entry" requested-entry-price* {:type :num :default (:requested-entry-price trade)})
       (field "Filled" filled-price* {:type :num :default (:filled-price trade)})
       (field "Init SL" initial-stop-price* {:type :num :default (:initial-stop-price trade)})
       (field "Cur SL" current-stop-price* {:type :num :default (:current-stop-price trade)})
       (field "Qty" quantity* {:type :num :default (:quantity trade)})
       (field "Rem qty" remaining-quantity* {:type :num :default (:remaining-quantity trade)})
       (field "Rem %" remaining-pct* {:type :num :default (:remaining-pct trade)})
       (field "Net R" net-r*)
       (field "Opened" opened-at*)
       (field "Updated" updated-at*)
       (field "Closed" closed-at*)
       (field "Close reason" close-reason*)
       (field "Broker" broker-ref*)
       (field "Pos ID" broker-position-id*)
       (field "Risk" rd-outcome*)
       (field "Profile" rd-profile-name*)
       (field "Violations" rd-violations* {:type :area :span 2 :rows 1})]
      [:table.simulator-table
       [:thead
        [:tr
         [:th "TP"]
         [:th "#"]
         [:th "Price"]
         [:th "%"]
         [:th "Hit"]]]
       [:tbody
        (map-indexed take-profit-row tp-signals)]]
      [:button.simulator-submit
       {:type "button"
        :data-on:click
        (h/action {:as "submit-signal"}
                  (submit-signal! submit-triggered* tp-signals
                                  trade-id* lifecycle-id* symbol* direction* status*
                                  source-protocol* entry-order-type* requested-entry-price*
                                  filled-price* initial-stop-price* current-stop-price*
                                  quantity* remaining-quantity* remaining-pct* net-r*
                                  opened-at* updated-at* closed-at* close-reason*
                                  broker-ref* broker-position-id* rd-outcome*
                                  rd-profile-name* rd-violations*))}
       "Submit"]
      [:p.simulator-toast
       {:data-show @submit-triggered*
        :style "display:none"}
       "triggered new signal"]]]))
