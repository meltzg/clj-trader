(ns clj-trader.components.date-selector
  (:require [rum.core :as rum]
            ["@mui/material" :refer [FormControl
                                     InputLabel
                                     Select
                                     MenuItem
                                     Stack]]))

(defn month-string [month-num]
  (.toLocaleString (js/Date. 0 month-num) "default" (clj->js {:month "long"})))

(defn days-in-month [year month-num]
  (.getDate (js/Date. year (+ 1 month-num) 0)))

(rum/defc date-selector [date on-change]
  [:> Stack {:direction   "row"
             :align-items "center"
             :spacing     0.25
             :padding-top 1}
   [:> FormControl {:sx {:m 1 :minWidth 90}}
    [:> InputLabel "Year"]
    [:> Select {:value    (.getFullYear date)
                :label    "Year"
                :onChange #(on-change (js/Date. (.. % -target -value)
                                                (.getMonth date)
                                                (.getDate date)))}
     (map (fn [year] [:> MenuItem {:key year :value year} year])
          (-> (range (- (.getFullYear (js/Date.)) 4)
                     (+ (.getFullYear (js/Date.)) 1))
              reverse))]]
   [:> FormControl {:sx {:m 1 :minWidth 125}}
    [:> InputLabel "Month"]
    [:> Select {:value    (.getMonth date)
                :label    "Month"
                :onChange #(on-change (js/Date. (.getFullYear date)
                                                (.. % -target -value)
                                                1))}
     (map (fn [month] [:> MenuItem {:key month :value month} (month-string month)])
          (range 12))]]
   [:> FormControl {:sx {:m 1 :minWidth 40}}
    [:> InputLabel "Day"]
    [:> Select {:value    (.getDate date)
                :label    "Day"
                :onChange #(on-change (js/Date. (.getFullYear date)
                                                (.getMonth date)
                                                (.. % -target -value)))}
     (map (fn [day] [:> MenuItem {:key day :value day} day])
          (range 1 (+ 1 (days-in-month (.getFullYear date) (.getMonth date)))))]]])
