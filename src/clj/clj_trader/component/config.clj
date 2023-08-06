(ns clj-trader.component.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]))

(defrecord Config [app-settings-file user-settings-file]
  component/Lifecycle

  (start [this]
    (let [app-settings-file (or app-settings-file
                                "app.edn")
          user-settings-from-file (if (.exists (io/file user-settings-file))
                                    (->> (slurp user-settings-file)
                                         edn/read-string)
                                    {})]
      (println "Loading app settings from" app-settings-file)
      (println "Loading user settings from" user-settings-file)
      (assoc this :config (->> (slurp app-settings-file)
                               edn/read-string
                               (merge {:host     "localhost"
                                       :port     8080
                                       :ssl-port 8443}))
                  :user-settings-file user-settings-file
                  :user-settings (atom (merge {:trading-freq-seconds 5
                                               :position-size        10}
                                              user-settings-from-file)))))

  (stop [this]
    (assoc this :config nil)))

(defn make-config [app-settings-file user-settings-file]
  (map->Config {:app-settings-file  app-settings-file
                :user-settings-file user-settings-file}))

(defn get-redirect-uri [config]
  (let [{:keys [host ssl-port]} config]
    (str "https://" host ":" ssl-port "/")))

(defn update-settings [{:keys [user-settings user-settings-file]} settings-patch]
  (reset! user-settings settings-patch)
  (spit user-settings-file @user-settings))
