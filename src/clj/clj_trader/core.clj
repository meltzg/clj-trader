(ns clj-trader.core
  (:require [compojure.core :refer :all]
            [clojure.string :refer [upper-case lower-case]]
            [compojure.route :as route]
            [ring.util.response :refer [response resource-response]]
            [ring.middleware.json :as json-mid]
            [ring.adapter.jetty :refer [run-jetty]])
  (:gen-class))

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

(defn start-server [join?]
  (run-jetty app {:port 8080 :join? join?}))

(defn -main
  "I don't do a whole lot ... yet."
  [& _]
  (start-server true))
