(ns clj-trader.utils
  (:require
    [sablono.util]))

(defn attrs [a]
  (clj->js (sablono.util/html-to-dom-attrs a)))
