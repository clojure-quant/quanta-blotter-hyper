(ns quanta.blotter-hyper.view.account-status
  (:require
   [missionary.core :as m]
   [hyper.core :as h]
   [quanta.missionary.task-timbre :refer [start-task!]]
   [quanta.blotter.oms.server :as oms-server]
   [quanta.blotter-hyper.view.orders :as orders-view]
   [quanta.blotter-hyper.view.positions :as positions-view]))

(defn close-dialog!
  [status-dialog-a]
  (reset! status-dialog-a nil))

(defn open-dialog!
  "Open status popup in :waiting, then fill from make-account-status-request."
  [oms-server status-dialog-a account-id]
  (h/action
   (reset! status-dialog-a {:account-id account-id :state :waiting})
   (start-task!
    (m/sp
     (let [{:keys [orders positions]}
           (m/? (oms-server/make-account-status-request oms-server account-id))]
       (swap! status-dialog-a
              (fn [d]
                (when (and d (= (:account-id d) account-id))
                  {:account-id account-id
                   :state :ready
                   :orders orders
                   :positions positions})))))
    (str "account-status-" account-id))))

(defn status-button
  [account {:keys [oms-server status-dialog-a]}]
  [:button.account-status-btn
   {:type "button"
    :data-on:click
    (open-dialog! oms-server status-dialog-a (:account/id account))}
   "status"])

(defn- section
  [title body]
  [:section.account-status-section
   [:h3 title]
   body])

(defn dialog
  [status-dialog-a]
  (when-let [{:keys [account-id state orders positions]} @status-dialog-a]
    [:div.account-status-backdrop
     [:div.account-status-dialog
      [:div.account-status-header
       [:h2 (str "Account " account-id " status")]
       [:button.account-status-close
        {:type "button"
         :data-on:click (h/action (close-dialog! status-dialog-a))}
        "Close"]]
      (if (= state :waiting)
        [:p.account-status-waiting "waiting"]
        [:div.account-status-body
         (section "Open positions"
                  (if (nil? positions)
                    [:p.account-status-timeout "timeout"]
                    (positions-view/positions-table positions)))
         (section "Working orders"
                  (if (nil? orders)
                    [:p.account-status-timeout "timeout"]
                    (orders-view/orders-table orders)))])]]))
