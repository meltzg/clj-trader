(ns clj-trader.component.api
  (:require [clj-time.coerce :as tc]
            [clj-trader.algo.indicators :as indicators]
            [clj-trader.component.config :as config]
            [clj-trader.component.td-brokerage :as td]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.defaults :as defaults]
            [ring.middleware.json :as json-mid]
            [ring.util.response :refer [resource-response response]]))

(defn- sign-in-handler [config {:keys [td-brokerage]} {:keys [body]}]
  (let [code (:code body)]
    (response (-> (td/execute-command {:command      :sign-in
                                       :code         code
                                       :config       config
                                       :td-brokerage td-brokerage})
                  (select-keys [:expires-at :refresh-expires-at :signed-in?])
                  (update-vals #(if (instance? org.joda.time.DateTime %)
                                  (tc/to-string %)
                                  %))))))

(defn- get-auth-status-handler [config {:keys [td-brokerage]} & _]
  (let [current-status (td/execute-command {:command      :auth-status
                                            :config       config
                                            :td-brokerage td-brokerage})
        final-status (if (:signed-in? current-status)
                       current-status
                       (td/execute-command {:command      :refresh-access-token
                                            :config       config
                                            :td-brokerage td-brokerage}))]
    (response (-> final-status
                  (select-keys [:expires-at :refresh-expires-at :signed-in?])
                  (update-vals #(if (instance? org.joda.time.DateTime %)
                                  (tc/to-string %)
                                  %))))))

(defn- sign-out-handler [config {:keys [td-brokerage]} & _]
  (response (-> (td/execute-command {:command      :sign-out
                                     :config       config
                                     :td-brokerage td-brokerage})
                (select-keys [:expires-at :refresh-expires-at :signed-in?])
                (update-vals #(if (instance? org.joda.time.DateTime %)
                                (tc/to-string %)
                                %)))))

(defn- price-history-handler [config {:keys [td-brokerage]} params & _]
  (response (td/execute-command {:command      :price-history
                                 :config       config
                                 :td-brokerage td-brokerage
                                 :params       (merge params
                                                      (update-vals
                                                        (select-keys params [:period-type :frequency-type])
                                                        keyword))})))

(defn- app-routes [td-brokerage config]
  (routes
    (GET "/api/oauthUri" [] (fn [_] (response {:oauth-uri (get-in td-brokerage [:td-brokerage :oauth-uri])})))
    (GET "/" [] (resource-response "index.html" {:root "public"}))
    (POST "/api/signIn" [] (partial sign-in-handler config td-brokerage))
    (GET "/api/authStatus" [] (partial get-auth-status-handler config td-brokerage))
    (GET "/api/signOut" [] (partial sign-out-handler config td-brokerage))
    (GET "/api/userSettings" [] (fn [_] (response (-> config
                                                      :user-settings
                                                      deref))))
    (PUT "/api/userSettings" [] (fn [{:keys [body]}]
                                  (config/update-settings config body)
                                  (response (-> config
                                                :user-settings
                                                deref))))
    (GET "/api/priceHistory" {params :params} (partial price-history-handler config td-brokerage params))
    (GET "/api/periodFrequencyInfo" [] (fn [_] (response td/period-frequency-info)))
    (GET "/api/indicatorConfigInfo" [] (fn [_] (response indicators/config-map)))
    (route/resources "/")
    (route/not-found "Not Found")))

(defrecord Api [config td-brokerage]
  component/Lifecycle

  (start [this]
    (let [{:keys [host port ssl-port keystore-path keystore-pass]} (:config config)]
      (println "Starting server on host" host " port: " port " ssl-port: " ssl-port)
      (assoc this :server (run-jetty
                            (-> (app-routes td-brokerage config)
                                (json-mid/wrap-json-body {:key-fn keyword})
                                json-mid/wrap-json-response
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
    (println "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
