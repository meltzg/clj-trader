(ns clj-trader.components.inputs
  (:require [rum.core :as rum]
            ["@mui/material" :refer [FormControl
                                     InputLabel
                                     MenuItem
                                     Select]]))

(rum/defc form-selector [{:keys [value label on-change items]}]
          [:> FormControl {:sx {:m 1 :minWidth 180}}
           [:> InputLabel label]
           [:> Select {:value    (if (keyword? value) (name value) value)
                       :label    label
                       :onChange on-change}
            (map (fn [item]
                   (let [item-value (if (keyword? item) (name item) item)]
                     [:> MenuItem {:key item :value item-value}
                      item-value]))
                 items)]])
