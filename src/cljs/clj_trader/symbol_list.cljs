(ns clj-trader.symbol-list
  (:require [clojure.string :refer [upper-case]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/material" :refer [IconButton
                                     List
                                     ListItem
                                     ListItemIcon
                                     ListItemText
                                     Stack
                                     TextField]]
            [rum.core :as rum]))

(defn handle-add-symbol [symbols on-change event]
  (when (= "Enter" (.-key event))
    (let [new-symbol (upper-case (.. event -target -value))]
      (on-change (-> symbols
                     (conj new-symbol)
                     set
                     sort))
      (set! (.. event -target -value) ""))))

(defn render-symbol-item [symbols on-change symbol]
  [:> ListItem {:key     symbol
                :divider true}
   [:> ListItemText {:primary symbol}]
   [:> ListItemIcon
    [:> IconButton {:color   "error"
                    :onClick #(on-change (->> symbols
                                              (remove #{symbol})
                                              set
                                              sort))}
     [:> DeleteIcon]]]])

(rum/defc symbol-list [symbols on-change]
  [:> Stack {:direction "column"}
   [:> TextField {:label     "Add New Symbol"
                  :onKeyDown (partial handle-add-symbol symbols on-change)}]
   [:> List
    (map (partial render-symbol-item symbols on-change) symbols)]])
