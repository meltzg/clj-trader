(ns clj-trader.core
  (:require [com.stuartsierra.component :as component]
            [clj-trader.component
             [api :as api]
             [config :as config]])
  (:gen-class))


(defn system [app-settings-file]
  (component/system-map
    :config (config/make-config app-settings-file)
    :api (component/using
           (api/make-api)
           [:config])))


(defn -main
  [& args]
  (let [system (component/start (system (first args)))
        lock (promise)
        stop (fn []
               (component/stop system)
               (deliver lock :release))]
    (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable stop))
    @lock
    (System/exit 0)))
