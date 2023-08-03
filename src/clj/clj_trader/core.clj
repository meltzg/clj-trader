(ns clj-trader.core
  (:require (clj-trader.component
              [api :as api]
              [config :as config]
              [td-brokerage :as td-brokerage])
            [com.stuartsierra.component :as component])
  (:gen-class))


(defn system [app-settings-file]
  (component/system-map
    :config (config/make-config app-settings-file)
    :td-brokerage (component/using
                    (td-brokerage/make-td)
                    [:config])
    :api (component/using
           (api/make-api)
           [:config
            :td-brokerage])))


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
