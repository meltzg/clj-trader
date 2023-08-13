(ns clj-trader.price-history
  (:require [ajax.core :as ajax]
            [clj-trader.mui-extension :as mui-x]
            [clj-trader.utils :as utils :refer [api-url]]
            [cljs-material-ui.core :as mui]
            ["@canvasjs/react-charts" :as CanvasJSReact]
            [rum.core :as rum]))

(def CanvasJSChart (.. CanvasJSReact -default -CanvasJSChart))

(def component-state (atom {:use-periods    false
                            :use-end-date   false
                            :period-type    :day
                            :periods        1
                            :frequency-type :minute
                            :frequency      1
                            :start-date     (utils/yesterday)
                            :end-date       nil
                            :chart-data     []}))

(def period-types
  [:day
   :month
   :year
   :ytd])

(def frequency-types
  [:minute
   :day
   :weekly
   :monthly])

(def valid-periods
  {:day   [1 2 3 4 5 10]
   :month [1 2 3 6]
   :year  [1 2 3 5 10 15 20]
   :ytd   [1]})

(def valid-frequency-type-for-period
  {:day   [:minute]
   :month [:day :weekly]
   :year  [:day :weekly :monthly]
   :ytd   [:day :weekly]})

(def valid-frequencies
  {:minute  [1]
   :day     [1 5 10 15 30]
   :weekly  [1]
   :monthly [1]})

(defn price-history->chart-data [{:keys [symbol price-candles]}]
  {:type               "candlestick"
   :showInLegend       true
   :name               symbol
   :yValueFormatString "$###0.00"
   :dataPoints         (mapv (fn [candle]
                               {:x (js/Date. (:datetime candle))
                                :y [(:open candle)
                                    (:high candle)
                                    (:low candle)
                                    (:close candle)]})
                             price-candles)})

(defn refresh-data []
  (ajax/GET (str api-url "priceHistory")
            {:format          :edn
             :response-format :json
             :keywords?       true
             :params          {:period-type    (:period-type @component-state)
                               :periods        (:periods @component-state)
                               :frequency-type (:frequency-type @component-state)
                               :frequency      (:frequency @component-state)}
             :handler         (fn [price-histories]
                                (swap! component-state
                                       assoc
                                       :chart-data
                                       (mapv price-history->chart-data price-histories)))}))

(rum/defc price-chart < [chart-data]
  [:> CanvasJSChart {:options {:title            {:text "Price History"}
                               :zoomEnabled      true
                               :animationEnabled true
                               :exportEnabled    true
                               :axis             {:prefix "$"
                                                  :title  "Price (USD)"}
                               :data             (clj->js chart-data)
                               }}])

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
         :on-change #(swap! component-state assoc :periods (.. % -target -value))}
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

(rum/defc price-history < rum/reactive [{:keys [symbols]}]
  [:div
   (price-chart (:chart-data (rum/react component-state)))
   (mui-x/stack
     {:direction "row" :spacing 1}
     (frequency-period-control)
     (mui/button
       {:variant  "contained"
        :on-click refresh-data}
       "Refresh"))])
