(ns clj-trader.component.config
  (:require [clojure.edn :as edn]
            [com.stuartsierra.component :as component]))

(def application-settings (atom {:host "localhost"
                                 :port 8080
                                 :ssl-port 8443}))

(defrecord Config [app-settings-file]
  component/Lifecycle

  (start [this]
    (let [app-settings-file (or app-settings-file
                                "app.edn")]
      (println "Loading app settings from" app-settings-file)
      (->> (slurp app-settings-file)
           edn/read-string
           (swap! application-settings merge))
      (assoc this :config @application-settings)))

  (stop [this]
    (assoc this :config nil)))

(defn make-config [app-settings-file]
  (map->Config {:app-settings-file app-settings-file}))

(defn get-redirect-uri [config]
  (let [{:keys [host port]} (:config config)]
    (str "https://" host ":" port "/")))
