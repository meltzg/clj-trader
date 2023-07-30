(ns clj-trader.component.api
  (:require [com.stuartsierra.component :as component]
            [compojure.core :refer :all]
            [clojure.string :refer [upper-case lower-case]]
            [compojure.route :as route]
            [ring.util.response :refer [response resource-response]]
            [ring.middleware.json :as json-mid]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn echo-handler [{:keys [body]}]
  (let [message (:message body)]
    (response {:message (str (upper-case message) message (lower-case message))})))

(defn- app-routes [td-brokerage]
  (routes
    (POST "/api/echo" [] echo-handler)
    (GET "/api/oauthUri" [] (fn [_] (response {:oauth-uri (get-in td-brokerage [:td-brokerage :oauth-uri])})))
    (GET "/" [] (resource-response "index.html" {:root "public"}))
    (route/resources "/")
    (route/not-found "Not Found")))

(defrecord Api [config td-brokerage]
  component/Lifecycle

  (start [this]
    (let [{:keys [host port]} (:config config)]
      (println "Starting server on host" host ":" port)
      (assoc this :server (run-jetty
                                 (-> (app-routes td-brokerage)
                                     (json-mid/wrap-json-body {:key-fn keyword})
                                     (json-mid/wrap-json-response))
                                 {:host host :port port :join? false}))))

  (stop [this]
    (println "Stopping server")
    (.stop (:server this))
    (assoc this :server nil)))

(defn make-api []
  (map->Api {}))
