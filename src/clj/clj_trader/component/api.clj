(ns clj-trader.component.api
  (:require [clj-time.coerce :as tc]
            [clj-trader.algo.indicators :as indicators]
            [clj-trader.component.config :as config]
            [clj-trader.component.td-brokerage :as td]
            [clj-trader.utils.values :refer [frequency-types period-types]]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [muuntaja.middleware :as mw]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.logger :as log-mw]
            [ring.middleware.defaults :as defaults]
            [ring.util.response :refer [resource-response response]]))

(defn- sign-in-handler [config-manager {:keys [td-brokerage]}]
  (fn [{:keys [body-params]}]
    (let [code (:code body-params)]
      (response (-> (td/execute-command {:command        :sign-in
                                         :code           code
                                         :config-manager config-manager
                                         :td-brokerage   td-brokerage})
                    (select-keys [:expires-at :refresh-expires-at :signed-in?])
                    (update-vals #(if (instance? org.joda.time.DateTime %)
                                    (tc/to-string %)
                                    %)))))))

(defn- get-auth-status-handler [config-manager {:keys [td-brokerage]}]
  (fn [& _]
    (let [current-status (td/execute-command {:command        :auth-status
                                              :config-manager config-manager
                                              :td-brokerage   td-brokerage})
          final-status (if (:signed-in? current-status)
                         current-status
                         (td/execute-command {:command        :refresh-access-token
                                              :config-manager config-manager
                                              :td-brokerage   td-brokerage}))]
      (response (-> final-status
                    (select-keys [:expires-at :refresh-expires-at :signed-in?])
                    (update-vals #(if (instance? org.joda.time.DateTime %)
                                    (tc/to-string %)
                                    %)))))))

(defn- sign-out-handler [config-manager {:keys [td-brokerage]}]
  (fn [& _]
    (response (-> (td/execute-command {:command        :sign-out
                                       :config-manager config-manager
                                       :td-brokerage   td-brokerage})
                  (select-keys [:expires-at :refresh-expires-at :signed-in?])
                  (update-vals #(if (instance? org.joda.time.DateTime %)
                                  (tc/to-string %)
                                  %))))))

(defn- price-history-handler [config-manager {:keys [td-brokerage]} params]
  (fn [& _]
    (response (td/execute-command {:command        :detailed-price-history
                                   :config-manager config-manager
                                   :td-brokerage   td-brokerage
                                   :params         (merge params
                                                          (update-vals
                                                            (select-keys params [:period-type :frequency-type])
                                                            keyword))}))))

(defn- update-user-settings-handler [config-manager]
  (fn [{:keys [body-params]}]
    (config/update-user-settings config-manager body-params)
    (response (config/get-user-settings config-manager))))

(defn- app-routes [td-brokerage config-manager]
  (routes
    (GET "/api/oauthUri" [] (fn [_] (response {:oauth-uri (get-in td-brokerage [:td-brokerage :oauth-uri])})))
    (GET "/" [] (resource-response "index.html" {:root "public"}))
    (POST "/api/signIn" [] (sign-in-handler config-manager td-brokerage))
    (GET "/api/authStatus" [] (get-auth-status-handler config-manager td-brokerage))
    (GET "/api/signOut" [] (sign-out-handler config-manager td-brokerage))
    (GET "/api/userSettings" [] (fn [_] (response (config/get-user-settings config-manager))))
    (PUT "/api/userSettings" [] (update-user-settings-handler config-manager))
    (GET "/api/priceHistory" {params :params} (price-history-handler config-manager td-brokerage params))
    (GET "/api/periodFrequencyInfo" [] (fn [_] (response (assoc td/period-frequency-info
                                                           :period-types period-types
                                                           :frequency-types frequency-types))))
    (GET "/api/indicatorConfigInfo" [] (fn [_] (response indicators/config-map)))
    (route/resources "/")
    (route/not-found "Not Found")))

(defrecord Api [config-manager td-brokerage]
  component/Lifecycle

  (start [this]
    (let [{:keys [host port ssl-port keystore-path keystore-pass]} (config/get-app-settings config-manager)]
      (logger/log :info (str "Starting server on host " host " port: " port " ssl-port: " ssl-port))
      (assoc this :server (run-jetty
                            (-> (app-routes td-brokerage config-manager)
                                log-mw/wrap-with-logger
                                mw/wrap-format
                                (defaults/wrap-defaults {:params {:keywordize true
                                                                  :urlencoded true}}))
                            {:ssl?         true
                             :host         host
                             :port         port
                             :ssl-port     ssl-port
                             :keystore     keystore-path
                             :key-password keystore-pass
                             :join?        false}))))

  (stop [this]
    (logger/log :info "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
