(ns clj-trader.user-settings
  (:require [clj-trader.api-client :as api]
            [cljs.core.async :refer [<! go]]
            [clojure.string :refer [upper-case]]
            [rum.core :as rum]))

(defn handle-submit [on-change symbol-list event]
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
                 (assoc :symbols symbol-list))]
    (go (let [updated-settings (<! (api/patch-user-settings data))]
          (on-change updated-settings)))))

(defn refresh-settings [current-settings on-change set-symbols-list!]
  (go (let [settings (<! (api/get-user-settings))]
        (when-not (= current-settings settings)
          (set-symbols-list! (:symbols settings))
          (on-change settings)))))

(defn render-symbols-list [symbol-list set-symbols-list!]
  [:ul
   (map (fn [symbol]
          [:li
           symbol
           [:button {:on-click #(set-symbols-list!
                                  (remove (fn [current]
                                            (= symbol current))
                                          symbol-list))}
            "Remove"]])
        symbol-list)])

(defn handle-type-symbol [symbol-list set-symbol-list! event]
  (when (= "Enter" (.-key event))
    (do
      (.preventDefault event)
      (set-symbol-list! (conj symbol-list (upper-case (.. event -target -value))))
      (set! (.. event -target -value) ""))))

(rum/defc settings-panel [user-settings change-settings]
  (let [[symbol-list set-symbol-list!] (rum/use-state [])]
    (refresh-settings user-settings change-settings set-symbol-list!)
    [:form {:method "post" :on-submit (partial handle-submit change-settings symbol-list)}
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
      [:input {:name "new-symbol"
               :on-key-down (partial handle-type-symbol
                                     symbol-list
                                     set-symbol-list!)}]]
     (render-symbols-list symbol-list set-symbol-list!)
     [:hr]
     [:button {:type "reset"} "Reset"]
     [:button {:type "submit"} "Save"]]))
