(ns clj-trader.api-client
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! go]]
            [clojure.string :as string]))

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/"))

(defn do-echo [message]
  (go (let [response (<! (http/post (str api-url "echo")
                                    {:json-params {:message message}}))]
        (get-in response [:body :message]))))

(defn get-oauth-uri []
  (go (let [response (<! (http/get (str api-url "oauthUri")))]
        (get-in response [:body :oauth-uri]))))

(defn sign-in [code]
  (go (let [response (<! (http/post (str api-url "signIn")
                                    {:json-params {:code code}}))]
        (:body response))))

(defn auth-status []
  (go (let [response (<! (http/get (str api-url "authStatus")))]
        (:body response))))

(defn sign-out []
  (go (let [response (<! (http/get (str api-url "signOut")))]
        (:body response))))
