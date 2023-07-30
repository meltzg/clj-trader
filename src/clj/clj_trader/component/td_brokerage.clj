(ns clj-trader.component.td-brokerage
  (:require [com.stuartsierra.component :as component]
            [clj-trader.component.config :refer [get-redirect-uri]]))

(defrecord TDBrokerage [config]
  component/Lifecycle

  (start [this]
    (assoc this :td-brokerage {:oauth-uri (str "https://auth.tdameritrade.com/auth?response_type=code&redirect_uri="
                                               (get-redirect-uri config)
                                               "&client_id="
                                               (get-in config [:config :client-id])
                                               "%40AMER.OAUTHAP")}))

  (stop [this]
    (assoc this :td-brokerage nil)))

(defn make-td []
  (map->TDBrokerage {}))
