(ns clj-trader.apps.user-settings
  (:require [ajax.core :as ajax]
            [clj-trader.components.symbol-list :refer [symbol-list]]
            [clj-trader.utils :refer [api-url]]
            ["@mui/material" :refer [Button
                                     FormControlLabel
                                     Stack
                                     Switch
                                     TextField]]
            [rum.core :as rum]))

(def component-state (atom {}))

(defn handle-save [on-change]
  (ajax/PUT (str api-url "userSettings")
            {:params          (:settings @component-state)
             :handler         (fn [data]
                                (swap! component-state assoc :settings data)
                                (on-change data))
             :format          :json
             :response-format :json
             :keywords?       true}))

(rum/defc settings-panel < rum/reactive [initial-settings change-settings]
  (when-not (:settings @component-state)
    (swap! component-state assoc :settings initial-settings))
  [:> Stack {:direction       "column"
             :spacing         1
             :justify-content "flex-start"
             :align-items     "stretch"}
   [:> FormControlLabel {:label   "Enable Automated Trading"
                         :control (rum/adapt-class Switch {:onChange #(swap! component-state
                                                                             assoc-in
                                                                             [:settings :enable-automated-trading]
                                                                             (.. % -target -checked))
                                                           :checked  (-> (rum/react component-state)
                                                                         :settings
                                                                         :enable-automated-trading)})}]
   [:> FormControlLabel {:label   "Enable End of Day Exit"
                         :control (rum/adapt-class Switch {:onChange #(swap! component-state
                                                                             assoc-in
                                                                             [:settings :end-of-day-exit]
                                                                             (.. % -target -checked))
                                                           :checked  (-> (rum/react component-state)
                                                                         :settings
                                                                         :end-of-day-exit)})}]
   [:> TextField {:label      "Trading Frequency Seconds"
                  :type       "number"
                  :InputProps {:inputProps {:min 1}}
                  :onChange   #(swap! component-state
                                      assoc-in
                                      [:settings :trading-freq-seconds]
                                      (.. % -target -valueAsNumber))
                  :value      (or (-> (rum/react component-state)
                                      :settings
                                      :trading-freq-seconds) "")}]
   [:> TextField {:label      "Position Size ($)"
                  :type       "number"
                  :InputProps {:inputProps {:min 0}}
                  :onChange   #(swap! component-state
                                      assoc-in
                                      [:settings :position-size]
                                      (.. % -target -valueAsNumber))
                  :value      (or (-> (rum/react component-state)
                                      :settings
                                      :position-size) "")}]
   (symbol-list (-> (rum/react component-state)
                    :settings
                    :tickers)
                #(swap! component-state assoc-in [:settings :tickers] %))
   [:> Stack {:direction "row"
              :spacing   1}
    [:> Button {:variant  "contained"
                :onClick  (fn [] (handle-save change-settings))
                :disabled (= (:settings (rum/react component-state))
                             initial-settings)}
     "Save"]
    [:> Button {:variant  "contained"
                :onClick  #(swap! component-state assoc :settings initial-settings)
                :disabled (= (:settings (rum/react component-state))
                             initial-settings)}
     "Reset"]]])
