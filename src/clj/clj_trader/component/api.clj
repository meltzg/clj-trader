(ns clj-trader.component.api
  (:require [clj-time.coerce :as tc]
            [clj-trader.component.td-brokerage :as td]
            [clojure.string :refer [lower-case upper-case]]
            [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :as json-mid]
            [ring.util.response :refer [resource-response response]]))

(defn- echo-handler [{:keys [body]}]
  (let [message (:message body)]
    (response {:message (str (upper-case message) message (lower-case message))})))

(defn- sign-in-handler [{:keys [config]} {:keys [td-brokerage]} {:keys [body]}]
  (let [code (:code body)]
    (response (-> (td/execute-command {:command      :sign-in
                                       :code         code
                                       :config       config
                                       :td-brokerage td-brokerage})
                  (select-keys [:expires-at :refresh-expires-at :signed-in?])
                  (update-vals #(if (instance? org.joda.time.DateTime %)
                                  (tc/to-string %)
                                  %))))))

(defn- get-auth-status-handler [{:keys [config]} {:keys [td-brokerage]} _]
  (response (-> (td/execute-command {:command      :auth-status
                                     :config       config
                                     :td-brokerage td-brokerage})
                (select-keys [:expires-at :refresh-expires-at :signed-in?])
                (update-vals #(if (instance? org.joda.time.DateTime %)
                                (tc/to-string %)
                                %)))))

(defn- sign-out-handler [{:keys [config]} {:keys [td-brokerage]} _]
  (response (-> (td/execute-command {:command      :sign-out
                                     :config       config
                                     :td-brokerage td-brokerage})
                (select-keys [:expires-at :refresh-expires-at :signed-in?])
                (update-vals #(if (instance? org.joda.time.DateTime %)
                                (tc/to-string %)
                                %)))))

(defn- app-routes [td-brokerage config]
  (routes
    (POST "/api/echo" [] echo-handler)
    (GET "/api/oauthUri" [] (fn [_] (response {:oauth-uri (get-in td-brokerage [:td-brokerage :oauth-uri])})))
    (GET "/" [] (resource-response "index.html" {:root "public"}))
    (POST "/api/signIn" [] (partial sign-in-handler config td-brokerage))
    (GET "/api/authStatus" [] (partial get-auth-status-handler config td-brokerage))
    (GET "/api/signOut" [] (partial sign-out-handler config td-brokerage))
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
                                (json-mid/wrap-json-response))
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
