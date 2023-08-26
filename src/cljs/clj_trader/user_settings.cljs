(ns clj-trader.user-settings
  (:require [ajax.core :as ajax]
            [clj-trader.utils :refer [api-url]]
            [clojure.string :refer [upper-case]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/material" :refer [Button
                                     FormControlLabel
                                     IconButton
                                     List
                                     ListItem
                                     ListItemIcon
                                     ListItemText
                                     Stack
                                     Switch
                                     TextField]]
            [rum.core :as rum]))

(def component-state (atom {:settings {}
                            :symbol   nil}))

(defn refresh-settings-mixin []
  {:will-mount
   (fn [state]
     (ajax/GET (str api-url "userSettings")
               {:handler         (fn [data]
                                   (swap! component-state assoc :settings data))
                :response-format :json
                :keywords?       true})
     state)})

(defn handle-save [onChange]
  (ajax/PUT (str api-url "userSettings")
            {:params          (:settings @component-state)
             :handler         (fn [data]
                                (swap! component-state assoc :settings data)
                                (onChange data))
             :format          :json
             :response-format :json
             :keywords?       true}))

(defn handle-type-symbol [event]
  (when (= "Enter" (.-key event))
    (do
      (swap! component-state update-in [:settings :symbols] #(sort (set (conj % (upper-case (.. event -target -value))))))
      (swap! component-state assoc :symbol nil))))

(defn render-symbol-item [symbol]
  [:> ListItem {:key     symbol
                :divider true}
   [:> ListItemText {:primary symbol}]
   [:> ListItemIcon
    [:> IconButton {:color   "error"
                    :onClick #(swap! component-state
                                     update-in
                                     [:settings :symbols]
                                     (fn [symbols]
                                       (remove #{symbol} symbols)))}
     [:> DeleteIcon]]]])

(rum/defc settings-panel < rum/reactive (refresh-settings-mixin) [initial-settings change-settings]
  (when (empty? initial-settings)
    (change-settings (:settings @component-state)))
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
   [:> TextField {:label     "Add New Symbol"
                  :onKeyDown handle-type-symbol
                  :onChange  #(swap! component-state
                                     assoc
                                     :symbol
                                     (.. % -target -value))
                  :value     (or (:symbol (rum/react component-state)) "")}]
   [:> List
    (map render-symbol-item (-> (rum/react component-state)
                                :settings
                                :symbols))]
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
