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
                              {:x (:datetime candle)
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
                                       (mapv price-history->chart-data price-histories))
                                (set! (.. (:chart-ref @component-state) -options -title -text) "FUCK")
                                (.render (:chart-ref @component-state)))}))

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

(rum/defc price-history < rum/reactive [{:keys [symbols]}]
  [:div
   [:> CanvasJSChart {:options {:title            {:text "Price History"}
                                :zoomEnabled      true
                                :animationEnabled true
                                :exportEnabled    true
                                :axis             {:prefix "$"
                                                   :title  "Price (USD)"}
                                :data             (:chart-data (rum/react component-state))
                                ;:data [{:type "candlestick", :showInLegend true, :name "AMZN", :yValueFormatString "$###0.00", :dataPoints [{:x 1689570000000, :y [134.56 135.99 128.415 130]} {:x 1690174800000, :y [130.305 133.01 126.11 132.21]} {:x 1690779600000, :y [133.2 143.63 126.41 139.57]} {:x 1691384400000, :y [140.99 142.54 137 138.41]}]} {:type "candlestick", :showInLegend true, :name "IBM", :yValueFormatString "$###0.00", :dataPoints [{:x 1689570000000, :y [133.26 140.32 133.1 138.94]} {:x 1690174800000, :y [139.35 143.95 138.7788 143.45]} {:x 1690779600000, :y [143.81 146.09 142.17 144.24]} {:x 1691384400000, :y [145 146.5 142.205 143.12]}]}]

                                }
                      :onRef #(swap! component-state assoc :chart-ref %)}]
   (mui-x/stack
     {:direction "row" :spacing 1}
     (frequency-period-control)
     (mui/button
       {:variant  "contained"
        :on-click refresh-data}
       "Refresh"))])
