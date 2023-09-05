(ns user
  (:require [com.stuartsierra.component :as component]
            [clj-trader.core :as core]
            [fiddle]))

(def sys (core/new-system "app.dev.edn" "user.dev.edn"))

(defn start-system []
  (alter-var-root #'sys component/start))

(defn stop-system []
  (alter-var-root #'sys component/stop))
