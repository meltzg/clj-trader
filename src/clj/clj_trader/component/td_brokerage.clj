(ns clj-trader.component.td-brokerage
  (:require [clj-trader.component.config :refer [get-redirect-uri]]
            [com.stuartsierra.component :as component]
            [clj-http.client :as http]
            [clj-time.core :as t]
            [clj-trader.utils.secrets :as secrets]
            [clojure.java.io :as io]
            [ring.util.codec :refer [form-encode]]))

(def api-root "https://api.tdameritrade.com/v1")
(def secrets-file "td-secrets.jks")
(def td-secrets (atom nil))

(defn- make-request [{:keys [method path options]}]
  (cond (= method :post)
        (http/post (str api-root path)
                   options)))

(defn- format-sign-in-response [{:keys [body]}]
  (let [{:keys [access_token
                expires_in
                refresh_token
                refresh_token_expires_in]} body]
    (println body)
    {:access-token       access_token
     :refresh-token      refresh_token
     :expires-at         (t/from-now (t/seconds expires_in))
     :refresh-expires-at (t/from-now (t/seconds refresh_token_expires_in))}))

(defn- save-access-info! [keystore-pass access-info]
  (let [save-secret (partial secrets/set-secret-in-keystore! @td-secrets keystore-pass)]
    (map #(apply save-secret %) access-info))
  (secrets/save-keystore! @td-secrets keystore-pass secrets-file))

(defn- load-access-info [keystore-pass]
  (into {} (map #(secrets/get-secret-from-keystore @td-secrets keystore-pass %)
                [:access-token
                 :refresh-token
                 :expires-at
                 :refresh-expires-at])))

(defmulti command->request :command)

(defmethod command->request :sign-in [{:keys [code config]}]
  (let [body {:grant_type   "authorization_code"
              :access_type  "offline"
              :code         code
              :client_id    (:client-id config)
              :redirect_uri (get-redirect-uri config)}]
    {:method  :post
     :path    "/oauth2/token"
     :options {:headers {:content-type "application/x-www-form-urlencoded"}
               :body    (form-encode body)}}))

(defmulti execute-command :command)

(defmethod execute-command :sign-in [command]
  (->> (command->request command)
       make-request
       format-sign-in-response
       (save-access-info! (get-in command [:config :keystore-pass]))))

(defrecord TDBrokerage [config]
  component/Lifecycle

  (start [this]
    (if (.exists (io/file secrets-file))
      (swap! td-secrets (fn [_] (secrets/load-keystore! secrets-file (get-in config [:config :keystore-pass]))))
      (do
        (swap! td-secrets (fn [_] (secrets/make-new-keystore (get-in config [:config :keystore-pass]))))
        (secrets/save-keystore! @td-secrets (get-in config [:config :keystore-pass]) secrets-file)))
    (assoc this :td-brokerage {:oauth-uri (str "https://auth.tdameritrade.com/auth?response_type=code&redirect_uri="
                                               (get-redirect-uri (:config config))
                                               "&client_id="
                                               (get-in config [:config :client-id])
                                               "%40AMER.OAUTHAP")}))

  (stop [this]
    (assoc this :td-brokerage nil)))

(defn make-td []
  (map->TDBrokerage {}))
