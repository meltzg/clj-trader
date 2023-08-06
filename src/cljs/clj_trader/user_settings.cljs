(ns clj-trader.user-settings
  (:require [clj-trader.api-client :as api]
            [cljs.core.async :refer [<! go]]
            [rum.core :as rum]))

(defn handle-submit [event]
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
    (prn data)))

(rum/defc settings-panel []
  [:form {:method "post" :on-submit handle-submit}
   [:label
    "Enable automated trading:"
    [:input {:type "checkbox" :name "enable-automated-trading" :value true}]]
   [:label
    "Enable end-of-day exit:"
    [:input {:type "checkbox" :name "end-of-day-exit" :value true}]]
   [:label
    "Trading frequency (seconds):"
    [:input {:type "number" :name "trading-freq-seconds" :min 1}]]
   [:label
    "Position size ($):"
    [:input {:type "number" :name "position-size" :min 1}]]
   [:hr]
   [:hr]
   [:button {:type "reset"} "Reset"]
   [:button {:type "submit"} "Save"]])
