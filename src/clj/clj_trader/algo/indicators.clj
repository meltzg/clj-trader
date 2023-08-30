(ns clj-trader.algo.indicators
  (:import (java.lang Math)))

(def config-map {:sma       {:name "Simple Moving Average"
                              :opts {:periods {:type :int
                                               :min  1}}}
                  :bollinger {:name "Bollinger Bands"
                              :opts {:periods      {:type :int
                                                    :min  1}
                                     :num-std-devs {:type :int
                                                    :min  1}}}})

(defn average [col]
  (/ (reduce + col) (count col)))

(defn standard-deviation [col]
  (let [avg (average col)]
    (Math/sqrt
      (/ (reduce + (map #(Math/pow (- % avg) 2) col))
         (- (count col) 1)))))

(defn bollinger-bands [num-std-devs col]
  (let [avg (average col)
        std-dev (standard-deviation col)
        deviation (* num-std-devs std-dev)]
    [(+ avg deviation)
     (- avg deviation)]))

(defmulti make-indicator :indicator)

(defmethod make-indicator :sma [_]
  average)

(defmethod make-indicator :bollinger [{:keys [num-std-devs]}]
  (partial bollinger-bands num-std-devs))

(defn moving-indicator [{:keys [price-candles price-key window-size] :as op}]
  (let [indicator (make-indicator op)]
    (->> price-candles
         (map price-key)
         (partition window-size 1)
         (map indicator))))
