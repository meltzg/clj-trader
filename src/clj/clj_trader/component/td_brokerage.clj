(ns clj-trader.component.td-brokerage
  (:require [clj-http.client :as http]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-trader.component.config :refer [get-redirect-uri]]
            [clj-trader.utils.secrets :as secrets]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ring.util.codec :refer [form-encode]]))

(def api-root "https://api.tdameritrade.com/v1")
(def secrets-file "td-secrets.jks")

(defn- make-request [{:keys [method path options]}]
  (cond (= method :post)
        (http/post (str api-root path)
                   options)))

(defn- format-sign-in-response [{:keys [body]}]
  (let [{:keys [access_token
                expires_in
                refresh_token
                refresh_token_expires_in]} body]
    {:access-token       access_token
     :refresh-token      refresh_token
     :expires-at         (t/from-now (t/seconds expires_in))
     :refresh-expires-at (t/from-now (t/seconds refresh_token_expires_in))}))

(defn save-access-info! [{:keys [keystore]} keystore-pass access-info]
  (let [access-info (assoc access-info :expires-at (tc/to-string (:expires-at access-info))
                                       :refresh-expires-at (tc/to-string (:refresh-expires-at access-info)))
        save-secret (partial secrets/set-secret-in-keystore! keystore keystore-pass)]
    (doall (map #(apply save-secret %) access-info)))
  (secrets/save-keystore! keystore keystore-pass secrets-file))

(defn load-access-info [{:keys [keystore]} keystore-pass]
  (if (not-every? true? (map #(.containsAlias keystore (name %))
                             [:access-token :refresh-token :expires-at :refresh-expires-at]))
    {:signed-in? false}
    (let [access-info (into {} (map (fn [key] {key (secrets/get-secret-from-keystore keystore keystore-pass key)})
                                    [:access-token
                                     :refresh-token
                                     :expires-at
                                     :refresh-expires-at]))
          expires-at (tc/from-string (:expires-at access-info))]
      (assoc access-info :signed-in? (t/after? expires-at (t/now))
                         :expires-at expires-at
                         :refresh-expires-at (tc/from-string (:refresh-expires-at access-info))))))

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
               :body    (form-encode body)
               :as      :json}}))

(defmulti execute-command :command)

(defmethod execute-command :sign-in [{:keys [td-brokerage config] :as command}]
  (->> (command->request command)
       make-request
       format-sign-in-response
       (save-access-info! td-brokerage (:keystore-pass config)))
  (load-access-info td-brokerage (:keystore-pass config)))

(defmethod execute-command :auth-status [{:keys [td-brokerage config]}]
  (load-access-info td-brokerage (:keystore-pass config)))

(defmethod execute-command :sign-out [{:keys [td-brokerage config]}]
  (doall (map #(.deleteEntry (:keystore td-brokerage) (name %))
              [:access-token :refresh-token :expires-at :refresh-expires-at]))
  (secrets/save-keystore! (:keystore td-brokerage) (:keystore-pass config) secrets-file)
  (load-access-info td-brokerage (:keystore-pass config)))

(defn load-or-create-keystore [keystore-pass]
  (if (.exists (io/file secrets-file))
    (secrets/load-keystore! secrets-file keystore-pass)
    (let [ks (secrets/make-new-keystore keystore-pass)]
      (secrets/save-keystore! ks keystore-pass secrets-file)
      ks)))

(defrecord TDBrokerage [config]
  component/Lifecycle

  (start [this]
    (assoc this :td-brokerage {:oauth-uri (str "https://auth.tdameritrade.com/auth?response_type=code&redirect_uri="
                                               (get-redirect-uri (:config config))
                                               "&client_id="
                                               (get-in config [:config :client-id])
                                               "%40AMER.OAUTHAP")
                               :keystore  (load-or-create-keystore (get-in config [:config :keystore-pass]))}))

  (stop [this]
    (assoc this :td-brokerage nil)))

(defn make-td []
  (map->TDBrokerage {}))
