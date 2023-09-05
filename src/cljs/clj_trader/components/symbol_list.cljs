(ns clj-trader.components.symbol-list
  (:require [clojure.string :refer [upper-case]]
            ["@mui/icons-material/Delete$default" :as DeleteIcon]
            ["@mui/material" :refer [IconButton
                                     List
                                     ListItem
                                     ListItemIcon
                                     ListItemText
                                     Stack
                                     Switch
                                     TextField]]
            [rum.core :as rum]))

(defn handle-add-ticker [tickers on-change event]
  (when (= "Enter" (.-key event))
    (let [new-ticker (upper-case (.. event -target -value))]
      (on-change (-> tickers
                     (conj new-ticker)
                     set
                     sort))
      (set! (.. event -target -value) ""))))

(defn render-symbol-item [tickers on-change toggleable enabled-tickers on-enable-change ticker]
  [:> ListItem {:key     ticker
                :divider true}
   [:> ListItemText {:primary ticker}]
   [:> ListItemIcon
    [:> Switch {:disabled (not toggleable)
                :checked  (some? (some #{ticker} enabled-tickers))
                :onChange #(on-enable-change (if (.. % -target -checked)
                                               (conj enabled-tickers ticker)
                                               (remove #{ticker} enabled-tickers)))}]
    [:> IconButton {:color   "error"
                    :onClick #(on-change (->> tickers
                                              (remove #{ticker})
                                              set
                                              sort))}
     [:> DeleteIcon]]]])

(rum/defc symbol-list [tickers on-change toggleable enabled-tickers on-enable-change]
  [:> Stack {:direction "column"}
   [:> TextField {:label     "Add New ticker"
                  :onKeyDown (partial handle-add-ticker tickers on-change)}]
   [:> List
    (map (partial render-symbol-item tickers on-change toggleable enabled-tickers on-enable-change) tickers)]])
