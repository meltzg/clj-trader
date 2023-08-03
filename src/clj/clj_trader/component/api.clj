(ns clj-trader.component.api
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [clojure.string :refer [upper-case lower-case]]
            [compojure.route :as route]
            [ring.util.response :refer [response resource-response]]
            [ring.middleware.json :as json-mid]
            [ring.adapter.jetty :refer [run-jetty]]
            [clj-trader.component.td-brokerage :as td]))

(defn- echo-handler [{:keys [body]}]
  (let [message (:message body)]
    (response {:message (str (upper-case message) message (lower-case message))})))

(defn- sign-in-handler [{:keys [config]} {:keys [body]}]
  (let [code (:code body)]
    (response (select-keys (td/execute-command {:command :sign-in
                                                :code    code
                                                :config  config})
                           [:expires-at :refresh-expires-at]))))

(defn- app-routes [td-brokerage config]
  (routes
    (POST "/api/echo" [] echo-handler)
    (GET "/api/oauthUri" [] (fn [_] (response {:oauth-uri (get-in td-brokerage [:td-brokerage :oauth-uri])})))
    (GET "/" [] (resource-response "index.html" {:root "public"}))
    (POST "/api/signIn" [] (partial sign-in-handler config))
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
