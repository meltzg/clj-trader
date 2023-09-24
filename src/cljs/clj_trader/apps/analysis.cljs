(ns clj-trader.apps.analysis
  (:require [ajax.core :as ajax]
            [clj-trader.components.date-selector :refer [date-selector]]
            [clj-trader.components.indicator-list :refer [indicator-list]]
            [clj-trader.components.inputs :refer [form-selector]]
            [clj-trader.components.symbol-list :refer [symbol-list]]
            [clj-trader.utils :as utils :refer [api-url]]
            ["@canvasjs/react-charts$default" :as CanvasJSReact]
            ["@mui/material" :refer [Box
                                     Button
                                     Divider
                                     Drawer
                                     FormControl
                                     FormControlLabel
                                     Stack
                                     Switch
                                     TableContainer
                                     Table
                                     TableRow
                                     TableHead
                                     TableCell
                                     TableBody
                                     Typography]]
            [goog.string :as gstring]
            [goog.string.format]
            [rum.core :as rum]))

(def CanvasJSChart (.-CanvasJSChart CanvasJSReact))

(def component-state (atom {:use-start-date  false
                            :use-end-date    false
                            :period-type     :day
                            :periods         1
                            :frequency-type  :minute
                            :frequency       1
                            :start-date      (utils/yesterday)
                            :end-date        (js/Date.)
                            :chart-data      []
                            :table-data      []
                            :tickers         []
                            :enabled-tickers []}))

(defn price-history->chart-data [{:keys [ticker price-candles]}]
  {:type               "candlestick"
   :showInLegend       true
   :name               ticker
   :yValueFormatString "$###0.00"
   :dataPoints         (mapv (fn [candle]
                               {:x (js/Date. (:datetime candle))
                                :y [(:open candle)
                                    (:high candle)
                                    (:low candle)
                                    (:close candle)]})
                             price-candles)})

(defn price-history->table-data [{:keys [ticker stats]}]
  (prn "STATS" stats)
  (assoc stats :ticker ticker))

(defn refresh-data []
  (ajax/GET (str api-url "priceHistory")
            {:format          :transit
             :response-format :transit
             :keywords?       true
             :params          (-> {:period-type    (:period-type @component-state)
                                   :periods        (:periods @component-state)
                                   :frequency-type (:frequency-type @component-state)
                                   :frequency      (:frequency @component-state)
                                   :tickers        (:enabled-tickers @component-state)}
                                  (conj (when (:use-start-date @component-state)
                                          [:start-date (.getTime (:start-date @component-state))]))
                                  (conj (when (:use-end-date @component-state)
                                          [:end-date (.getTime (:end-date @component-state))])))
             :handler         (fn [price-histories]
                                (swap! component-state
                                       assoc
                                       :chart-data
                                       (mapv price-history->chart-data price-histories)
                                       :table-data
                                       (mapv price-history->table-data price-histories)))}))

(defn handle-legend-click [e]
  (if (or (nil? (.. e -dataSeries -visible))
          (true? (.. e -dataSeries -visible)))
    (set! (.. e -dataSeries -visible) false)
    (set! (.. e -dataSeries -visible) true))
  (.render (.-chart e)))

(defn handle-save-settings [settings on-change-settings]
  (ajax/PUT (str api-url "userSettings")
            {:params          settings
             :handler         on-change-settings
             :format          :transit
             :response-format :transit
             :keywords?       true}))

(defn handle-add-indicator [user-settings on-settings-change indicator-key indicator-options]
  (let [indicator-key (if (some #{indicator-key} (keys (:indicator-config-info @component-state)))
                        (keyword (.randomUUID js/crypto))
                        indicator-key)]
    (handle-save-settings
      (assoc-in user-settings [:indicators indicator-key] indicator-options)
      on-settings-change)
    (prn (cljs.pprint.pprint (:indicators @component-state)))))

(defn handle-delete-indicator [user-settings on-settings-change indicator-key]
  (handle-save-settings
    (update user-settings :indicators dissoc indicator-key)
    on-settings-change))

(rum/defc price-chart [chart-data]
  [:> CanvasJSChart {:options {:title            {:text "Price History"}
                               :zoomEnabled      true
                               :animationEnabled true
                               :exportEnabled    true
                               :axis             {:prefix "$"
                                                  :title  "Price (USD)"}
                               :legend           {:cursor    "pointer"
                                                  :itemclick handle-legend-click}
                               :data             (clj->js chart-data)}}])

(rum/defc stats-table [stats-data]
  [:> TableContainer
   [:> Table
    [:> TableHead
     [:> TableRow {:sx {:backgroundColor "lightgray"}}
      (concat [[:> TableCell "Symbol"]]
              (map (fn [column] [:> TableCell {:align "right" :key column}
                                 (name column)])
                   (->> stats-data first keys (remove #{:ticker}) sort)))]]
    [:> TableBody
     (map (fn [row] [:> TableRow {:key (:ticker row)}
                     (concat [[:> TableCell (:ticker row)]]
                             (map (fn [key] [:> TableCell {:align "right"} (gstring/format "%.2f" (key row))])
                                  (->> stats-data first keys (remove #{:ticker}) sort)))])
          stats-data)]]])

(rum/defc frequency-period-control < rum/reactive [period-frequency-info]
  [:> Stack {:direction  "row"
             :alignItems "center"
             :spacing    1
             :paddingTop 1}
   (form-selector {:value     (:period-type (rum/react component-state))
                   :label     "Period Type"
                   :on-change #(swap! component-state assoc :period-type (keyword (.. % -target -value)))
                   :items     (:period-types period-frequency-info)})
   (form-selector {:value     (:periods (rum/react component-state))
                   :label     "# Periods"
                   :on-change #(swap! component-state assoc :periods (.. % -target -value))
                   :items     ((:period-type (rum/react component-state)) (:valid-periods period-frequency-info))})
   (form-selector {:value     (:frequency-type (rum/react component-state))
                   :label     "Frequency Type"
                   :on-change #(swap! component-state assoc :frequency-type (keyword (.. % -target -value)))
                   :items     ((:period-type (rum/react component-state)) (:valid-frequency-type-for-period period-frequency-info))})
   (form-selector {:value     (:frequency (rum/react component-state))
                   :label     "Frequency"
                   :on-change #(swap! component-state assoc :frequency (.. % -target -value))
                   :items     ((:frequency-type (rum/react component-state)) (:valid-frequencies period-frequency-info))})])

(rum/defc start-end-control < rum/reactive []
  [:> Stack {:direction "column" :spacing 0.5}
   [:> Stack {:direction "row" :spacing 0.5}
    [:> FormControlLabel {:label   "Use Start Date"
                          :control (rum/adapt-class Switch {:onChange #(swap! component-state
                                                                              assoc
                                                                              :use-start-date
                                                                              (.. % -target -checked))
                                                            :checked  (:use-start-date (rum/react component-state))})}]
    (when (:use-start-date (rum/react component-state))
      (date-selector (:start-date (rum/react component-state))
                     #(swap! component-state assoc :start-date %)))]
   [:> Stack {:direction "row" :soacing 0.5}
    [:> FormControlLabel {:label   "Use End Date"
                          :control (rum/adapt-class Switch {:onChange #(swap! component-state
                                                                              assoc
                                                                              :use-end-date
                                                                              (.. % -target -checked))
                                                            :checked  (:use-end-date (rum/react component-state))})}]
    (when (:use-end-date (rum/react component-state))
      (date-selector (:end-date (rum/react component-state))
                     #(swap! component-state assoc :end-date %)))]])

(rum/defc chart-settings [period-frequency-info]
  [:> Stack {:direction "column" :spacing 0.5}
   (start-end-control)
   (frequency-period-control period-frequency-info)])

(rum/defc analysis-settings < rum/reactive [user-settings indicator-config-info on-settings-change]
  (swap! component-state assoc :indicator-config-info indicator-config-info)
  [:> Stack {:direction "column" :spacing 0.5}
   [:> Typography {:variant "h6" :component "div" :sx {:flexGrow 1}}
    "Symbols"]
   (symbol-list (:tickers (rum/react component-state))
                #(swap! component-state assoc :tickers %)
                true
                (:enabled-tickers (rum/react component-state))
                #(swap! component-state assoc :enabled-tickers %))
   [:> Button {:variant  "contained"
               :disabled (= (:tickers user-settings) (:tickers (rum/react component-state)))
               :onClick  #(swap! component-state assoc
                                 :tickers (:tickers user-settings)
                                 :enabled-tickers (:tickers user-settings))}
    "Reset from settings"]
   [:> Divider {}]
   [:> Typography {:variant "h6" :component "div" :sx {:flexGrow 1}}
    "Indicators"]
   (indicator-list
     (:indicators user-settings)
     indicator-config-info
     (partial handle-add-indicator user-settings on-settings-change)
     (partial handle-delete-indicator user-settings on-settings-change))])

(rum/defc analysis-app < rum/reactive [user-settings period-frequency-info indicator-config-info on-settings-change]
  (when (empty? (:tickers @component-state))
    (swap! component-state assoc :tickers (:tickers user-settings) :enabled-tickers (:tickers user-settings)))
  [:div.wrapper
   [:div.side-bar
    (analysis-settings user-settings indicator-config-info on-settings-change)]
   [:div.main-view
    [:> Stack {:direction "row" :spacing 1}
     (price-chart (:chart-data (rum/react component-state)))
     (stats-table (:table-data (rum/react component-state)))]]
   [:div.footer
    [:> Stack {:direction "row" :spacing 1}
     (chart-settings period-frequency-info)
     [:> Button {:variant "contained"
                 :onClick refresh-data}
      "Refresh"]]]])
