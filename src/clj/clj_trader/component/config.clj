(ns clj-trader.component.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :refer [pprint]]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]))

(defrecord ConfigManager [app-settings-file user-settings-file]
  component/Lifecycle

  (start [this]
    (let [app-settings-file (or app-settings-file
                                "app.edn")
          user-settings-from-file (if (.exists (io/file user-settings-file))
                                    (->> (slurp user-settings-file)
                                         edn/read-string)
                                    {})]
      (logger/log :info (str "Loading app settings from " app-settings-file))
      (logger/log :info (str "Loading user settings from " user-settings-file))
      (assoc this :app-settings (->> (slurp app-settings-file)
                                     edn/read-string
                                     (merge {:host     "localhost"
                                             :port     8080
                                             :ssl-port 8443}))
                  :user-settings-file user-settings-file
                  :user-settings (atom (merge {:trading-freq-seconds 5
                                               :position-size        10}
                                              user-settings-from-file)))))

  (stop [this]
    (assoc this :app-settings nil
                :user-settings nil
                :user-settings-file nil)))

(defn make-config [app-settings-file user-settings-file]
  (map->ConfigManager {:app-settings-file  app-settings-file
                       :user-settings-file user-settings-file}))

(defn update-user-settings [{:keys [user-settings user-settings-file]} settings-patch]
  (reset! user-settings settings-patch)
  (spit user-settings-file (with-out-str (pprint @user-settings))))

(defn get-user-settings [{:keys [user-settings]}]
  @user-settings)

(defn get-app-settings [{:keys [app-settings]}]
  app-settings)

(defn get-redirect-uri [config-manager]
  (let [{:keys [host ssl-port]} (get-app-settings config-manager)]
    (str "https://" host ":" ssl-port "/")))
