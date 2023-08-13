(ns clj-trader.utils.helpers)

(defn ?assoc [m k v]
  (conj m (when v [k v])))
