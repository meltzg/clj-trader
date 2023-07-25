(ns clj-trader.core
  (:require [com.stuartsierra.component :as component]
            [clj-trader.api :as api])
  (:gen-class))


(defn clj-system []
  (component/system-map :api (api/->Api "0.0.0.0" 8080)))


(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  (component/start-system (clj-system)))
