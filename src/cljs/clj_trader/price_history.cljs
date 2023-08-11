(ns clj-trader.price-history
  (:require [clj-trader.mui-extension :as mui-x]
            [clj-trader.utils :as utils :refer [api-url]]
            [cljs-material-ui.core :as mui]
            [rum.core :as rum]
            ["@canvasjs/react-charts" :as CanvasJSReact]))

(def CanvasJSChart (.. CanvasJSReact -default -CanvasJSChart))

(prn (rum/adapt-class DateTimePicker {}))

(def component-state (atom {:use-periods    true
                            :use-end-date   false
                            :period-type    :day
                            :periods        1
                            :frequency-type :minute
                            :frequency      1
                            :start-date     (utils/yesterday)
                            :end-date       nil
                            :price-data     []
                            :chart-data     []}))

(def period-types
  [:day
   :month
   :year
   :ytd])

(def frequency-types
  [:minute
   :daily
   :weekly
   :monthly])

(def valid-periods
  {:day   [1 2 3 4 5 10]
   :month [1 2 3 6]
   :year  [1 2 3 5 10 15 20]
   :ytd   [1]})

(def valid-frequency-type-for-period
  {:day   [:minute]
   :month [:daily :weekly]
   :year  [:daily :weekly :monthly]
   :ytd   [:daily :weekly]})

(def valid-frequencies
  {:minute  [1]
   :daily   [1 5 10 15 30]
   :weekly  [1]
   :monthly [1]})

(rum/defc frequency-period-control < rum/reactive []
  (mui-x/stack
    {:direction "row" :spacing 1 :padding-top 1}
    (mui/form-control
      {:sx {:m 1 :minWidth 180}}
      (mui/input-label "Period Type")
      (mui/select
        {:value     (:period-type (rum/react component-state))
         :label     "Period Type"
         :on-change #(swap! component-state assoc :period-type (keyword (.. % -target -value)))}
        (map #(mui/menu-item {:key % :value %} (name %)) period-types)))
    (mui/form-control
      {:sx {:m 1 :minWidth 90}}
      (mui/input-label "# Periods")
      (mui/select
        {:value     (:periods (rum/react component-state))
         :label     "# Periods"
         :on-change #(swap! component-state assoc :period-type (.. % -target -value))}
        (map #(mui/menu-item {:key % :value %} %) ((:period-type (rum/react component-state)) valid-periods))))
    (mui/form-control
      {:sx {:m 1 :minWidth 180}}
      (mui/input-label "Frequency Type")
      (mui/select
        {:value     (:frequency-type (rum/react component-state))
         :label     "Frequency Type"
         :on-change #(swap! component-state assoc :frequency-type (keyword (.. % -target -value)))}
        (map #(mui/menu-item {:key % :value %} (name %)) ((:period-type (rum/react component-state)) valid-frequency-type-for-period))))
    (mui/form-control
      {:sx {:m 1 :minWidth 90}}
      (mui/input-label "Frequency")
      (mui/select
        {:value     (:frequency (rum/react component-state))
         :label     "Frequency"
         :on-change #(swap! component-state assoc :frequency (.. % -target -value))}
        (map #(mui/menu-item {:key % :value %} %) ((:frequency-type (rum/react component-state)) valid-frequencies))))))

(rum/defc start-end-control < rum/reactive []
  (mui-x/stack
    {:direction "row" :spacing 1}
    ))

(rum/defc price-history [{:keys [symbols]}]
  [:div
   [:> CanvasJSChart {:options {:title {:text "Hello World"}
                                :data  [{:type       "column"
                                         :dataPoints [{:x 10 :y 71}
                                                      {:x 20 :y 55}]}]}}]
   (frequency-period-control)
   (start-end-control)])
