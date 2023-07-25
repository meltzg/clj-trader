(ns clj-trader.api-client
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]))

(defonce api-url (str (.-href (.-location js/window)) "api/"))


(defn do-echo [message]
  (go (let [response (<! (http/post (str api-url "echo")
                                    {:json-params {:message message}}))]
        (prn response)
        (get-in response [:body :message]))))
