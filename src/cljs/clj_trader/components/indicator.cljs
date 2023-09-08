(ns clj-trader.components.indicator
  (:require [rum.core :as rum]
            ["@mui/icons-material/ExpandMore$default" :as ExpandMoreIcon]
            ["@mui/material" :refer [Accordion
                                     AccordionDetails
                                     AccordionSummary
                                     Typography]]))

(rum/defc indicator-config [config indicator-info on-change]
  [:> Accordion
   [:> AccordionSummary {:expandIcon (rum/adapt-class ExpandMoreIcon {})}
    [:> Typography (:name indicator-info)]]
   [:> AccordionDetails
    [:> Typography config]]])
