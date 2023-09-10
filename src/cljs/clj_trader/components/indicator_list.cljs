(ns clj-trader.components.indicator-list
  (:require [rum.core :as rum]
            ["@mui/icons-material/Add$default" :as AddIcon]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Accordion
                                     AccordionDetails
                                     AccordionSummary
                                     Fab
                                     Menu
                                     MenuItem
                                     Stack
                                     Typography]]))

(def component-state (atom {}))

(defn handle-indicator-menu [event]
  (swap! component-state assoc :anchor-element (.-currentTarget event)))

(defn handle-indicator-close []
  (swap! component-state dissoc :anchor-element))

(defn add-indicator [indicator-key indicator-options]
  (handle-indicator-close)
  (swap! component-state assoc-in
         [:indicators (keyword (gensym (name indicator-key)))]
         {:opts indicator-options}))

(rum/defc indicator-config [config indicator-info on-change]
  [:> Accordion
   [:> AccordionSummary {:expandIcon (rum/adapt-class ExpandMoreIcon {})}
    [:> Typography (:name indicator-info)]]
   [:> AccordionDetails
    [:> Typography config]]])

(rum/defc indicator-list < rum/reactive [indicator-configs indicator-config-info on-change]
  [:> Stack {:direction "column"}
   (map (fn [[_ indicator]]
          (indicator-config (:config indicator) (:opts indicator) #())) indicator-configs)
   [:> Fab {:color         "primary"
            :size          "small"
            :id            "add-indicator"
            :aria-controls (when (:anchor-element (rum/react component-state)) "indicator-menu")
            :aria-haspopup "true"
            :aria-expanded (when (:anchor-element (rum/react component-state)) "true")
            :onClick       handle-indicator-menu}
    [:> AddIcon {}]]
   [:> Menu {:id            "indicator-menu"
             :anchorEl      (:anchor-element (rum/react component-state))
             :open          (some? (:anchor-element (rum/react component-state)))
             :onClose       handle-indicator-close
             :MenuListProps {:aria-labelledby "add-indicator"}}
    (map (fn [[indicator-key indicator-options]]
           [:> MenuItem {:onClick (fn []
                                    (handle-indicator-close)
                                    (on-change indicator-key indicator-options))}
            (:name indicator-options)]) indicator-config-info)]])
