(ns fiddle
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pprint]]))

(defn remove-bad-chars [values]
  (map #(string/replace % #"[^a-z0-9-]" "") values))

(defn replace-spaces [values]
  (map #(string/replace % #" " "-") values))

(defn lower-case-all [values]
  (map string/lower-case values))

(defn parse [data-lines]
  (let [columns (map keyword
                     (remove-bad-chars
                       (replace-spaces
                         (lower-case-all
                           (string/split (first data-lines) #",")))))
        data-vals (map #(string/split % #",") (rest data-lines))]
    (into [] (map #(zipmap columns %) data-vals))))


(def data (parse
            (string/split
              (slurp "https://www.fdic.gov/resources/resolutions/bank-failures/failed-bank-list/banklist.csv")
              #"\r?\n")))

(def failed-by-state (into {}
                           (map (fn [[state banks]] [state (count banks)])
                                (group-by :state data))))



(defn parse-with-threading [data-lines]
  (let [columns (->> (string/split (first data-lines) #",")
                     lower-case-all
                     replace-spaces
                     remove-bad-chars
                     (map keyword))
        data-vals (map #(string/split % #",") (rest data-lines))]
    (into [] (map #(zipmap columns %) data-vals))))

(def data-with-threading (-> (slurp "https://www.fdic.gov/resources/resolutions/bank-failures/failed-bank-list/banklist.csv")
                             (string/split #"\r?\n")
                             parse-with-threading))

(def failed-by-state-with-threading (->> data-with-threading
                                         (group-by :state)
                                         (map (fn [[state banks]] [state (count banks)]))
                                         (into {})))
