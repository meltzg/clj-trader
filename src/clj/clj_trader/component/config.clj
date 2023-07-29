(ns clj-trader.component.config
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as comp]))

(def application-settings (atom {:host "localhost"
                                 :port 8080}))

(defrecord Config [app-settings-file]
  comp/Lifecycle

  (start [component]
    (let [app-settings-file (or app-settings-file
                                "app.edn")]
      (println "Loading app settings from" app-settings-file)
      (->> (slurp app-settings-file)
           edn/read-string
           (swap! application-settings merge))
      (assoc component :config @application-settings)))

  (stop [component]
    (assoc component :config nil)))

(defn make-config [app-settings-file]
  (map->Config {:app-settings-file app-settings-file}))
