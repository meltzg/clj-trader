(ns clj-trader.user-settings
  (:require [clj-trader.api-client :as api]
            [cljs.core.async :refer [<! go]]
            [rum.core :as rum]))

(defn handle-submit [on-change event]
  (.preventDefault event)
  (let [form-data (->> event
                       .-target
                       js/FormData.
                       .entries
                       (.fromEntries js/Object))
        data (-> (js->clj form-data :keywordize-keys true)
                 (update :enable-automated-trading #(if % (parse-boolean %) false))
                 (update :end-of-day-exit #(if % (parse-boolean %) false))
                 (update :trading-freq-seconds #(if-not (empty? %) (parse-long %) 0))
                 (update :position-size #(if-not (empty? %) (parse-long %) 0)))]
    (go (let [updated-settings (<! (api/patch-user-settings data))]
          (on-change updated-settings)))))

(defn refresh-settings [current-settings on-change]
  (go (let [settings (<! (api/get-user-settings))]
        (when-not (= current-settings settings)
          (on-change settings)))))

(rum/defc settings-panel [user-settings change-settings]
  (refresh-settings user-settings change-settings)
  [:form {:method "post" :on-submit (partial handle-submit change-settings)}
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
    [:input {:type  "number"
             :name  "trading-freq-seconds"
             :default-value (:trading-freq-seconds user-settings)
             :min   1}]]
   [:label
    "Position size ($):"
    [:input {:type  "number"
             :name  "position-size"
             :default-value (:position-size user-settings)
             :min   1}]]
   [:hr]
   [:hr]
   [:button {:type "reset"} "Reset"]
   [:button {:type "submit"} "Save"]])
