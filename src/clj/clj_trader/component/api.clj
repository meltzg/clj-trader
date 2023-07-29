(ns clj-trader.component.api
  (:require [com.stuartsierra.component :as comp]
            [compojure.core :refer :all]
            [clojure.string :refer [upper-case lower-case]]
            [compojure.route :as route]
            [ring.util.response :refer [response resource-response]]
            [ring.middleware.json :as json-mid]
            [ring.adapter.jetty :refer [run-jetty]]))

(defn echo-handler [{:keys [body]}]
  (let [message (:message body)]
    (response {:message (str (upper-case message) message (lower-case message))})))

(defroutes app-routes
           (POST "/api/echo" [] echo-handler)
           (GET "/" [] (resource-response "index.html" {:root "public"}))
           (route/resources "/")
           (route/not-found "Not Found"))

(def app (-> app-routes
             (json-mid/wrap-json-body {:key-fn keyword})
             (json-mid/wrap-json-response)))

(defrecord Api [config]
  comp/Lifecycle

  (start [component]
    (let [{:keys [host port]} (:config config)]
      (println "Starting server on host" host ":" port)
      (assoc component :server (run-jetty app {:host host :port port :join? false}))))

  (stop [component]
    (println "Stopping server")
    (.stop (:server component))
    (assoc component :server nil)))

(defn make-api []
  (map->Api {}))
