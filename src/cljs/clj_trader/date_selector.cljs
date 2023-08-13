(ns clj-trader.date-selector
  (:require [clj-trader.mui-extension :as mui-x]
            [cljs-material-ui.core :as mui]
            [rum.core :as rum]))

(defn month-string [month-num]
  (.toLocaleString (js/Date. 0 month-num) "default" (clj->js {:month "long"})))

(defn days-in-month [year month-num]
  (.getDate (js/Date. year (+ 1 month-num) 0)))

(rum/defc date-selector [date on-change]
  (mui-x/stack
    {:direction   "row"
     :align-items "center"
     :spacing     0.25
     :padding-top 1}
    (mui/form-control
      {:sx {:m 1 :minWidth 90}}
      (mui/input-label "Year")
      (mui/select
        {:value     (.getFullYear date)
         :label     "Year"
         :on-change #(on-change (js/Date. (.. % -target -value)
                                          (.getMonth date)
                                          (.getDate date)))}
        (map #(mui/menu-item {:key % :value %} %)
             (-> (range (- (.getFullYear (js/Date.)) 4)
                        (+ (.getFullYear (js/Date.)) 1))
                 reverse))))
    (mui/form-control
      {:sx {:m 1 :minWidth 125}}
      (mui/input-label "Month")
      (mui/select
        {:value     (.getMonth date)
         :label     "Month"
         :on-change #(on-change (js/Date. (.getFullYear date)
                                          (.. % -target -value)
                                          1))}
        (map #(mui/menu-item {:key % :value %} (month-string %))
             (range 12))))
    (mui/form-control
      {:sx {:m 1 :minWidth 40}}
      (mui/input-label "Day")
      (mui/select
        {:value     (.getDate date)
         :label     "Day"
         :on-change #(on-change (js/Date. (.getFullYear date)
                                          (.getMonth date)
                                          (.. % -target -value)))}
        (map #(mui/menu-item {:key % :value %} %)
             (range 1 (+ 1 (days-in-month (.getFullYear date) (.getMonth date)))))))))
