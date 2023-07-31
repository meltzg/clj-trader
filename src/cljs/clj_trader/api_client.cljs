(ns clj-trader.api-client
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [clojure.string :as string]))

(defonce api-url (string/split (str (.-href (.-location js/window)) "api/")
                               #"\?"))


(defn do-echo [message]
  (go (let [response (<! (http/post (str api-url "echo")
                                    {:json-params {:message message}}))]
        (prn response)
        (get-in response [:body :message]))))

(defn get-oauth-uri []
  (go (let [response (<! (http/get (str api-url "oauthUri")))]
        (get-in response [:body :oauth-uri]))))
