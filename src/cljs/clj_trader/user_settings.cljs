(ns clj-trader.user-settings
  (:require [clj-trader.api-client :as api]
            [cljs.core.async :refer [<! go]]
            [clojure.string :refer [upper-case]]
            [rum.core :as rum]
            [cljs-material-ui.core :as mui]
            [clj-trader.mui-extension :as mui-x]
            [ajax.core :refer [GET PUT]]))

(def component-state (atom {:settings {}
                            :symbol   nil}))

(defn refresh-settings-mixin []
  {:will-mount
   (fn [state]
     (let [*data (atom nil)
           comp (:rum/react-component state)]
       (GET (str api/api-url "userSettings")
            {:handler         (fn [data]
                                (reset! *data data)
                                (swap! component-state assoc :settings data)
                                (rum/request-render comp))
             :response-format :json
             :keywords?       true})
       (assoc state :initial-settings *data)))})

(defn handle-save []
  (PUT (str api/api-url "userSettings")
       {:params (:settings @component-state)
        :handler         (fn [data]
                           (swap! component-state assoc :settings data)
                           (swap! component-state assoc :initial-settings data))
        :format          :json
        :response-format :json
        :keywords?       true}))

(defn handle-type-symbol [event]
  (when (= "Enter" (.-key event))
    (do
      (swap! component-state update-in [:settings :symbols] #(sort (set (conj % (upper-case (.. event -target -value))))))
      (swap! component-state assoc :symbol nil)
      (prn @component-state))))

(defn render-symbol-item [symbol]
  (mui/list-item
    {:key symbol}
    (mui/list-item-text {:primary symbol})
    (mui/button {:variant  "outlined"
                 :color    "error"
                 :on-click #(swap! component-state
                                   update-in
                                   [:settings :symbols]
                                   (fn [symbols]
                                     (remove #{symbol} symbols)))}
                "X")))

(rum/defcs settings-panel < rum/reactive (refresh-settings-mixin) [state user-settings change-settings]
  (let [initial-settings @(:initial-settings state)]
    (swap! component-state assoc :initial-settings initial-settings)
    (mui-x/stack
      {:direction       "column"
       :spacing         1
       :justify-content "flex-start"
       :align-items     "baseline"}
      (mui/form-control-label {:label   "Enable Automated Trading"
                               :control (mui/switch {:on-change #(swap! component-state
                                                                        assoc-in
                                                                        [:settings :enable-automated-trading]
                                                                        (.. % -target -checked))
                                                     :checked   (-> (rum/react component-state)
                                                                    :settings
                                                                    :enable-automated-trading)})})
      (mui/form-control-label {:label   "Enable End of Day Exit"
                               :control (mui/switch {:on-change #(swap! component-state
                                                                        assoc-in
                                                                        [:settings :end-of-day-exit]
                                                                        (.. % -target -checked))
                                                     :checked   (-> (rum/react component-state)
                                                                    :settings
                                                                    :end-of-day-exit)})})
      (mui/textfield {:label       "Trading Frequency Seconds"
                      :type        "number"
                      :Input-Props {:inputProps {:min 1}}
                      :on-change   #(swap! component-state
                                           assoc-in
                                           [:settings :trading-freq-seconds]
                                           (.. % -target -valueAsNumber))
                      :value       (or (-> (rum/react component-state)
                                           :settings
                                           :trading-freq-seconds) "")})
      (mui/textfield {:label       "Position Size ($)"
                      :type        "number"
                      :Input-Props {:inputProps {:min 0}}
                      :on-change   #(swap! component-state
                                           assoc-in
                                           [:settings :position-size]
                                           (.. % -target -valueAsNumber))
                      :value       (or (-> (rum/react component-state)
                                           :settings
                                           :position-size) "")})
      (mui/textfield {:label       "Add New Symbol"
                      :on-key-down handle-type-symbol
                      :on-change   #(swap! component-state
                                           assoc
                                           :symbol
                                           (.. % -target -value))
                      :value       (or (:symbol (rum/react component-state)) "")})
      (mui/list
        (map render-symbol-item (-> (rum/react component-state)
                                    :settings
                                    :symbols)))
      (mui-x/stack
        {:direction "row"
         :spacing   1}
        (mui/button {:variant  "contained"
                     :on-click handle-save}
                    "Save")
        (mui/button {:variant  "contained"
                     :on-click #(swap! component-state assoc :settings (:initial-settings @component-state))}
                    "Reset")))))
