(ns clj-trader.algo.indicators
  (:require [clj-trader.utils.values :refer [frequency-types]])
  (:import (java.lang Math)))

(def config-map {:sma       {:name "Simple Moving Average"
                             :opts {:window-type {:type         :choice
                                                  :display-name "Window type"
                                                  :opts         frequency-types}
                                    :window-size {:type         :int
                                                  :display-name "Window size"
                                                  :min          1}}}
                 :bollinger {:name "Bollinger Bands"
                             :opts {:window-type  {:type         :choice
                                                   :display-name "Window type"
                                                   :opts         frequency-types}
                                    :window-size  {:type         :int
                                                   :display-name "Window size"
                                                   :min          1}
                                    :num-std-devs {:type         :int
                                                   :display-name "Std deviations"
                                                   :min          1}}}})

(defn average [col]
  (/ (reduce + col) (count col)))

(defn standard-deviation [col]
  (let [avg (average col)]
    (Math/sqrt
      (/ (reduce + (map #(Math/pow (- % avg) 2) col))
         (- (count col) 1)))))

(defn bollinger-bands [num-std-devs price-key price-candles]
  (let [prices (map price-key price-candles)
        avg (average prices)
        std-dev (standard-deviation prices)
        deviation (* num-std-devs std-dev)]
    [(apply max (map :datetime price-candles))
     (+ avg deviation)
     (- avg deviation)]))

(defmulti make-indicator :type)

(defmethod make-indicator :sma [_]
  (fn [price-key price-candles]
    [(apply max (map :datetime price-candles))
     (average (map price-key price-candles))]))

(defmethod make-indicator :bollinger [{:keys [num-std-devs]}]
  (partial bollinger-bands num-std-devs))

(defn moving-indicator [{:keys [price-candles price-key opts] :as op}]
  (let [indicator (make-indicator op)]
    (->> price-candles
         (partition (-> opts :window-size :value) 1)
         (map (partial indicator price-key)))))
