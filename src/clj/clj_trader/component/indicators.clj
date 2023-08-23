(ns clj-trader.component.indicators)

(defn average [col]
  (/ (apply + col) (count col)))

(defn calculate-indicator [price-candles price-key window-size fn]
  (->> price-candles
       (map price-key)
       (partition window-size 1)
       (map fn)))
