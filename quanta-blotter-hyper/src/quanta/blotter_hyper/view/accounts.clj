(ns quanta.blotter-hyper.view.accounts
  (:require
   [clojure.edn :as edn]
   [clojure.string :as str]
   [datahike.api :as d]
   [hyper.core :as h]
   [hyper.effects :as effects]
   [quanta.blotter-hyper.view.common :as common]
   [quanta.blotter.oms.db :as db]))

(defn- parse-positive-int
  "Parse a string as a positive integer, or nil when invalid."
  [s]
  (when-let [s (not-empty (str/trim (str s)))]
    (when (re-matches #"[0-9]+" s)
      (let [n (parse-long s)]
        (when (pos? n) n)))))

(defn- update-account-balance!
  [conn account-id balance]
  (when-let [account (db/account-by-id conn account-id)]
    (d/transact conn [{:db/id (:db/id account)
                      :account/balance (bigdec balance)}])))

(defn- finish-edit!
  [editing-a]
  (reset! editing-a nil)
  (effects/execute-script! "window.__accountEditSavePending=false"))

(defn- cancel-edit!
  [editing-a account-id field]
  (let [e @editing-a]
    (when (and (= (:account-id e) account-id) (= (:field e) field))
      (finish-edit! editing-a))))

(defn save-account-name!
  [db-conn account-id name query-a editing-a]
  (let [name (str/trim name)]
    (when (seq name)
      (db/update-account db-conn {:account/id account-id :account/name name})
      (when query-a (swap! query-a update :n (fnil inc 0)))
      (effects/execute-script! "quantaBeep()"))
    (finish-edit! editing-a)))

(defn save-account-balance!
  [db-conn account-id raw-value query-a editing-a]
  (if-let [balance (parse-positive-int raw-value)]
    (do
      (update-account-balance! db-conn account-id balance)
      (when query-a (swap! query-a update :n (fnil inc 0)))
      (effects/execute-script! "quantaBeep()")
      (finish-edit! editing-a))
    nil))

(defn save-account-enabled!
  [db-conn account-id enabled query-a]
  (db/enable-account db-conn account-id enabled)
  (when query-a (swap! query-a update :n (fnil inc 0)))
  (effects/execute-script! "quantaBeep()"))

(defn save-account-asset-list!
  [db-conn account-id asset-list-id query-a]
  (when (pos? asset-list-id)
    (db/update-account db-conn {:account/id account-id
                                :account/asset-list asset-list-id})
    (when query-a (swap! query-a update :n (fnil inc 0)))
    (effects/execute-script! "quantaBeep()")))

(defn- account-asset-list-id [account]
  (let [asset-list (:account/asset-list account)]
    (cond
      (number? asset-list) asset-list
      (map? asset-list) (:db/id asset-list)
      :else nil)))

(defn- account-asset-list-name [account]
  (let [asset-list (:account/asset-list account)]
    (cond
      (map? asset-list) (:lists/name asset-list)
      :else nil)))

(defn- editing?
  [editing-a account-id field]
  (let [e @editing-a]
    (and (= (:account-id e) account-id) (= (:field e) field))))

(defn- edit-keydown
  [save-fn editing-a account-id field]
  (h/expr (when (or (= evt.key "Escape")
                    (and (= evt.key "Enter")
                         (set! (.-__accountEditSavePending js/window) true)))
          (h/action (if (= $key "Escape")
                      (cancel-edit! editing-a account-id field)
                      (save-fn))))))

(defn- edit-blur
  [editing-a account-id field cell-class]
  (h/expr (when (and (not (.-__accountEditSavePending js/window))
                     (not (and (.-relatedTarget evt)
                               (.closest (.-relatedTarget evt) (str "." cell-class)))))
          (h/action (cancel-edit! editing-a account-id field)))))

(defn- focus-edit-input!
  [selector]
  (effects/execute-script!
   (str "requestAnimationFrame(() => document.querySelector('" selector "')?.focus())")))

(defn- start-edit!
  [editing-a edit-value-a account-id field value input-selector]
  (h/action
   (reset! editing-a {:account-id account-id :field field})
   (reset! edit-value-a value)
   (focus-edit-input! input-selector)))

(defn- balance-edit-value
  [account]
  (str (long (bigdec (or (:account/balance account) 0M)))))

(defn- account-name-cell
  [account {:keys [editing-a edit-value-a db query-a]}]
  (let [account-id (:account/id account)
        field :name]
    [:td.account-name-cell
     (if (editing? editing-a account-id field)
       [:input.account-name-input
        {:type "text"
         :value (str @edit-value-a)
         :data-on:input (h/action (reset! edit-value-a $value))
         :data-on:keydown (edit-keydown #(save-account-name! db account-id @edit-value-a query-a editing-a)
                                        editing-a account-id field)
         :data-on:blur (edit-blur editing-a account-id field "account-name-cell")}]
       [:button.account-name-label
        {:type "button"
         :data-on:click (start-edit! editing-a edit-value-a account-id field
                                     (or (:account/name account) "") ".account-name-input")}
        (common/fmt-cell (:account/name account))])]))

(defn- account-balance-cell
  [account {:keys [editing-a edit-value-a db query-a]}]
  (let [account-id (:account/id account)
        field :balance]
    [:td.account-balance-cell.num
     (if (editing? editing-a account-id field)
       [:input.account-balance-input
        {:type "text"
         :inputmode "numeric"
         :pattern "[0-9]+"
         :value (str @edit-value-a)
         :data-on:input (h/action (reset! edit-value-a (str/replace $value #"[^0-9]" "")))
         :data-on:keydown (edit-keydown #(save-account-balance! db account-id @edit-value-a query-a editing-a)
                                        editing-a account-id field)
         :data-on:blur (edit-blur editing-a account-id field "account-balance-cell")}]
       [:button.account-balance-label
        {:type "button"
         :data-on:click (start-edit! editing-a edit-value-a account-id field
                                     (balance-edit-value account) ".account-balance-input")}
        (common/fmt-cell (:account/balance account))])]))

(defn- account-enabled-cell
  [account {:keys [db query-a]}]
  (let [account-id (:account/id account)
        enabled? (boolean (:account/enabled account))]
    [:td.account-enabled-cell
     [:label.account-enabled-slider
      [:input.account-enabled-input
       {:type "checkbox"
        :checked enabled?
        :data-on:change (h/action (save-account-enabled! db account-id $checked query-a))}]
      [:span.account-enabled-slider-track]]]))

(defn- account-asset-list-cell
  [account {:keys [db query-a asset-lists]}]
  (let [account-id (:account/id account)
        selected-id (account-asset-list-id account)]
    [:td.account-asset-list-cell
     [:select {:value (str (or selected-id ""))
               :data-on:change
               (h/action (save-account-asset-list! db account-id (parse-long $value) query-a))}
      (for [{:keys [db/id] :as asset-list} asset-lists
            :let [list-name (:lists/name asset-list)]]
        [:option {:key id
                  :value (str id)
                  :selected (= id selected-id)}
         list-name])]]))

(defn- fmt-settings [settings]
  (if (some? settings)
    (pr-str settings)
    "—"))

(defn- settings-text-value
  [settings]
  (cond
    (map? settings) (pr-str settings)
    (and (string? settings) (seq (str/trim settings))) settings
    :else "{}"))

(defn- settings-validation-error
  [s]
  (try
    (let [v (edn/read-string (str/trim s))]
      (when-not (map? v) "Settings must be a map"))
    (catch Exception e
      (str "Invalid EDN: " (.getMessage e)))))

(defn- parse-settings-map
  [s]
  (when-not (settings-validation-error s)
    (edn/read-string (str/trim s))))

(defn- close-settings-dialog!
  [settings-dialog-a settings-text-a settings-error-a]
  (reset! settings-dialog-a nil)
  (reset! settings-text-a "")
  (reset! settings-error-a nil))

(defn- open-settings-dialog!
  [settings-dialog-a settings-text-a settings-error-a account]
  (h/action
   (let [text (settings-text-value (:account/settings account))]
     (reset! settings-dialog-a {:account-id (:account/id account)})
     (reset! settings-text-a text)
     (reset! settings-error-a (settings-validation-error text))
     (effects/execute-script!
      "requestAnimationFrame(() => document.querySelector('.account-settings-text')?.focus())"))))

(defn save-account-settings!
  [db-conn account-id text query-a settings-dialog-a settings-text-a settings-error-a]
  (when-let [settings (parse-settings-map text)]
    (db/update-account db-conn {:account/id account-id :account/settings settings})
    (when query-a (swap! query-a update :n (fnil inc 0)))
    (effects/execute-script! "quantaBeep()")
    (close-settings-dialog! settings-dialog-a settings-text-a settings-error-a)))

(defn- parse-settings-from-account
  [settings]
  (cond
    (map? settings) settings
    (and (string? settings) (seq (str/trim settings)))
    (try (edn/read-string settings) (catch Exception _ nil))
    :else nil))

(defn- account-settings-dialog
  [{:keys [settings-dialog-a settings-text-a settings-error-a db query-a]}]
  (when-let [account-id (:account-id @settings-dialog-a)]
    [:div.account-settings-backdrop
     [:div.account-settings-dialog
      [:h2 (str "Account " account-id " settings")]
      [:textarea.account-settings-text
       {:rows 16
        :spellcheck false
        :data-on:input
        (h/action
         (reset! settings-text-a $value)
         (reset! settings-error-a (settings-validation-error $value)))}
       (str @settings-text-a)]
      (when-let [error @settings-error-a]
        [:p.account-settings-error error])
      [:div.account-settings-actions
       [:button.account-settings-cancel
        {:type "button"
         :data-on:click
         (h/action (close-settings-dialog! settings-dialog-a
                                            settings-text-a
                                            settings-error-a))}
        "Cancel"]
       [:button.account-settings-save
        {:type "button"
         :disabled (some? @settings-error-a)
         :data-on:click
         (h/action (save-account-settings! db account-id @settings-text-a query-a
                                           settings-dialog-a settings-text-a
                                           settings-error-a))}
        "Save"]]]]))

(defn- account-settings-cell
  [account {:keys [settings-dialog-a settings-text-a settings-error-a]}]
  [:td.settings
   [:button.account-settings-btn
    {:type "button"
     :data-on:click
     (open-settings-dialog! settings-dialog-a settings-text-a settings-error-a account)}
    "settings"]])

(defn- account-row
  [account {:keys [show-trader? editable?] :as opts}]
  [:tr {:key (:account/id account)}
   (when show-trader?
     [:td (common/fmt-cell (:account/trader account))])
   [:td (common/fmt-cell (:account/id account))]
   (if editable?
     (account-name-cell account opts)
     [:td (common/fmt-cell (:account/name account))])
   (if editable?
     (account-balance-cell account opts)
     [:td.num (common/fmt-cell (:account/balance account))])
   (if editable?
     (account-enabled-cell account opts)
     [:td (common/fmt-cell (:account/enabled account))])
   [:td (common/fmt-cell (:account/api account))]
   (if editable?
     (account-asset-list-cell account opts)
     [:td (common/fmt-cell (account-asset-list-name account))])
   (if editable?
     (account-settings-cell account opts)
     [:td.settings (fmt-settings (:account/settings account))])])

(defn accounts-table
  ([accounts]
   (accounts-table accounts {}))
  ([accounts opts]
   (let [{:keys [show-trader? editable? editing-a edit-value-a
                 settings-dialog-a settings-text-a settings-error-a
                 db query-a asset-lists]
          :or {show-trader? false
               asset-lists []}} opts
         table-opts (cond-> {:show-trader? show-trader?}
                       editable?
                       (assoc :editing-a editing-a
                              :edit-value-a edit-value-a
                              :settings-dialog-a settings-dialog-a
                              :settings-text-a settings-text-a
                              :settings-error-a settings-error-a
                              :db db
                              :query-a query-a
                              :asset-lists asset-lists
                              :editable? true))
         dialog-opts (select-keys table-opts [:settings-dialog-a :settings-text-a
                                              :settings-error-a :db :query-a])
         sort-key (if show-trader?
                    (juxt :account/trader :account/id)
                    :account/id)
         accounts (sort-by sort-key accounts)
         cols (if show-trader? 8 7)]
     [:div.orders-table-wrap
      (when editable? (account-settings-dialog dialog-opts))
      [:table.orders-table
       [:thead
        [:tr
         (when show-trader? [:th "trader"])
         [:th "account id"]
         [:th "account name"]
         [:th.num "account balance"]
         [:th "enabled"]
         [:th "api type"]
         [:th "asset-list"]
         [:th "settings"]]]
       [:tbody
        (if (empty? accounts)
          [:tr [:td {:colspan cols} "No accounts"]]
          (map #(account-row % table-opts) accounts))]]])))

(defn- parse-account [account]
  (update account :account/settings parse-settings-from-account))

(defn query-all-asset-lists [conn]
  (sort-by :lists/name
           (d/q '[:find [(pull ?e [:db/id :lists/name]) ...]
                  :where [?e :lists/name _]]
                @conn)))

(defn query-all-accounts [conn]
  (->> (d/q '[:find [(pull ?e [:db/id :account/id :account/trader :account/name
                                :account/balance :account/enabled :account/api
                                :account/settings
                                {:account/asset-list [:db/id :lists/name]}]) ...]
              :where [?e :account/id _]]
            @conn)
       (map parse-account)))

(defn query-trader-accounts [conn trader]
  (->> (d/q '[:find [(pull ?e [:db/id :account/id :account/trader :account/name
                                :account/balance :account/enabled :account/api
                                :account/settings
                                {:account/asset-list [:db/id :lists/name]}]) ...]
              :in $ ?trader
              :where [?e :account/trader ?trader]]
            @conn trader)
       (map parse-account)))

(defn query-accounts [conn {:keys [trader]}]
  (if trader
    (query-trader-accounts conn trader)
    (query-all-accounts conn)))

(defn trader-account-id-set
  "When trader is a name, query accounts and return a set of :account/id.
   When trader is nil, return an empty set."
  [conn trader]
  (if trader
    (into #{} (map :account/id) (query-trader-accounts conn trader))
    #{}))

(defn account-id-pred
  "Datalog predicate for backoffice filtering by trader accounts.
   Admin (trader nil) matches all account ids; otherwise uses set membership."
  [conn trader]
  (let [account-ids (trader-account-id-set conn trader)]
    (if trader
      (partial contains? account-ids)
      (constantly true))))
