(ns clj-trader.price-history
  (:require [clj-trader.utils :as utils]
            [rum.core :as rum]
            ["@canvasjs/react-charts" :as CanvasJSReact]))

(def CanvasJS (.. CanvasJSReact -default -CanvasJS))
(def CanvasJSChart (.. CanvasJSReact -default -CanvasJSChart))

(rum/defc price-history []
  [:div
   (rum/adapt-class CanvasJSChart {:options {:title {:text "Hello World"}
                                             :data [{:type "column"
                                                     :dataPoints [{:x 10 :y 71}
                                                                  {:x 20 :y 55}]}]}})])
