(ns clj-trader.user-settings
  (:require [clj-trader.api-client :as api]
            [cljs.core.async :refer [<! go]]
            [clojure.string :refer [upper-case]]
            [rum.core :as rum]))

(def component-state (atom {:symbol-list     []
                            :tmp-symbol-list []}))

(defn handle-submit [on-change event]
  (.preventDefault event)
  (let [form-data (->> event
                       .-target
                       js/FormData.
                       .entries
                       (.fromEntries js/Object))
        data (-> (js->clj form-data :keywordize-keys true)
                 (dissoc :new-symbol)
                 (update :enable-automated-trading #(if % (parse-boolean %) false))
                 (update :end-of-day-exit #(if % (parse-boolean %) false))
                 (update :trading-freq-seconds #(if-not (empty? %) (parse-long %) 0))
                 (update :position-size #(if-not (empty? %) (parse-long %) 0))
                 (assoc :symbols (:tmp-symbol-list @component-state)))]
    (go (let [updated-settings (<! (api/patch-user-settings data))]
          (on-change updated-settings)))))

(defn handle-reset [_]
  (swap! component-state assoc :tmp-symbol-list (:symbol-list @component-state)))

(defn refresh-settings [current-settings on-change]
  (go (let [settings (<! (api/get-user-settings))]
        (when-not (= current-settings settings)
          (swap! component-state assoc :symbol-list (:symbols settings))
          (swap! component-state assoc :tmp-symbol-list (:symbols settings))
          (on-change settings)))))

(defn render-symbols-list [symbol-list]
  [:ul
   (map (fn [symbol]
          [:li
           symbol
           [:button {:on-click (fn [event]
                                 (.preventDefault event)
                                 (swap! component-state update :tmp-symbol-list
                                        (fn [symbols]
                                          (remove #{symbol} symbols))))}
            "Remove"]])
        symbol-list)])

(defn handle-type-symbol [event]
  (when (= "Enter" (.-key event))
    (do
      (.preventDefault event)
      (swap! component-state update :tmp-symbol-list #(sort (set (conj % (upper-case (.. event -target -value))))))
      (set! (.. event -target -value) ""))))

(rum/defc settings-panel < rum/reactive [user-settings change-settings]
  (refresh-settings user-settings change-settings)
  [:form {:method    "post"
          :on-submit (partial handle-submit change-settings)
          :on-reset  handle-reset}
   [:label
    "Enable automated trading:"
    [:input {:type            "checkbox"
             :name            "enable-automated-trading"
             :default-checked (:enable-automated-trading user-settings)
             :value           true}]]
   [:label
    "Enable end-of-day exit:"
    [:input {:type            "checkbox"
             :name            "end-of-day-exit"
             :default-checked (:end-of-day-exit user-settings)
             :value           true}]]
   [:label
    "Trading frequency (seconds):"
    [:input {:type          "number"
             :name          "trading-freq-seconds"
             :default-value (:trading-freq-seconds user-settings)
             :min           1}]]
   [:label
    "Position size ($):"
    [:input {:type          "number"
             :name          "position-size"
             :default-value (:position-size user-settings)
             :min           1}]]
   [:hr]
   [:label
    "Add symbol"
    [:input {:name        "new-symbol"
             :on-key-down handle-type-symbol}]]
   (render-symbols-list (:tmp-symbol-list (rum/react component-state)))
   [:hr]
   [:button {:type "reset"} "Reset"]
   [:button {:type "submit"} "Save"]])
