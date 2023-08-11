(ns clj-trader.utils
  (:require [clojure.string :as string]))

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/"))

(defn yesterday []
  (let [day (js/Date.)]
    (.setDate day (- (.getDate day) 1))
    day))
