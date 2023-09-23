(ns clj-trader.components.indicator-list
  (:require [rum.core :as rum]
            ["@mui/icons-material/Add$default" :as AddIcon]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Accordion
                                     AccordionDetails
                                     AccordionSummary
                                     Fab
                                     FormControl
                                     InputLabel
                                     Menu
                                     MenuItem
                                     Select
                                     Stack
                                     TextField
                                     Typography]]))

(def component-state (atom {}))

(defn handle-indicator-menu [event]
  (swap! component-state assoc :anchor-element (.-currentTarget event)))

(defn handle-indicator-close []
  (swap! component-state dissoc :anchor-element))

(defmulti option-control #(-> %2 :type keyword))

(defmethod option-control :choice [opt-key {:keys [display-name opts]}]
  [:> FormControl {:fullWidth true}
   [:> InputLabel display-name]
   [:> Select {:label display-name}
    (map (fn [option]
           [:> MenuItem {:key option :value (name option)}
            (name option)])
         opts)]])

(defmethod option-control :int [opt-key {:keys [display-name min max]}]
  [:> TextField {:label display-name
                 :type  "number"}])

(rum/defc indicator-config [indicator-key indicator-info on-change]
  (prn "key" indicator-key)
  (prn "opts" (:opts indicator-info))
  [:> Accordion
   [:> AccordionSummary {:expandIcon (rum/adapt-class ExpandMoreIcon {})}
    [:> Typography (:name indicator-info)]]
   [:> AccordionDetails {}
    (map #(apply option-control %) (:opts indicator-info))]])

(rum/defc indicator-list < rum/reactive [indicator-configs indicator-config-info on-add]
  [:> Stack {:direction "column"}
   (map (fn [[indicator-key indicator]]
          (indicator-config indicator-key (:opts indicator) #())) indicator-configs)
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
                                    (on-add indicator-key indicator-options))}
            (:name indicator-options)]) indicator-config-info)]])
