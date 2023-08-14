(ns clj-trader.component.td-brokerage
  (:require [clj-http.client :as http]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-trader.component.config :refer [get-redirect-uri]]
            [clj-trader.utils.helpers :refer [?assoc]]
            [clj-trader.utils.secrets :as secrets]
            [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [ring.util.codec :refer [form-encode]]))

(def api-root "https://api.tdameritrade.com/v1")
(def secrets-file "td-secrets.jks")

(defmulti command->request :command)
(defmulti execute-command :command)

(defn- format-sign-in-response [{:keys [access_token
                                        expires_in
                                        refresh_token
                                        refresh_token_expires_in]}]
  {:access-token       access_token
   :refresh-token      refresh_token
   :expires-at         (t/from-now (t/seconds expires_in))
   :refresh-expires-at (t/from-now (t/seconds refresh_token_expires_in))})

(defn- format-price-history [{:keys [symbol candles]}]
  {:symbol        symbol
   :price-candles (map #(select-keys % [:open :high :low :close :datetime]) candles)})

(defn- calc-stat [price-candles keys stat-prefix f]
  (into {} (map (fn [key]
                  [(str stat-prefix (name key))
                   (f price-candles key)])
                keys)))

(defn- calc-stats [{:keys [price-candles] :as price-history}]
  (assoc price-history :stats (merge (calc-stat price-candles
                                                [:open :high :low :close]
                                                "avg-"
                                                (fn [price-candles key]
                                                  (/ (apply + (map key price-candles)) (count price-candles)))))))

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

(defn- make-request [{:keys [method path options]}]
  (let [request (merge options {:method                method
                                :url                   (str api-root path)
                                :throw-entire-message? true})]
    (println "make request" (update-in request [:headers :authorization] some?))
    (http/request request)))

(defn- make-authenticated-request
  ([td-brokerage config request]
   (make-authenticated-request td-brokerage config request true))
  ([td-brokerage config request refresh-tokens?]
   (let [{:keys [access-token]} (load-access-info td-brokerage (-> config :config :keystore-pass))]
     (try
       (make-request (assoc-in request [:options :headers :authorization]
                               (str "Bearer " access-token)))
       (catch Exception e
         (if (and refresh-tokens? (= 401 (-> e ex-data :status)))
           (do
             (println "refresh token and retry")
             (execute-command {:command      :refresh-access-token
                               :td-brokerage td-brokerage
                               :config       config})
             (make-authenticated-request td-brokerage config request false))
           (throw e)))))))

(defmethod command->request :sign-in [{:keys [code config]}]
  (let [body {:grant_type   "authorization_code"
              :access_type  "offline"
              :code         code
              :client_id    (-> config :config :client-id)
              :redirect_uri (get-redirect-uri config)}]
    {:method  :post
     :path    "/oauth2/token"
     :options {:headers {:content-type "application/x-www-form-urlencoded"}
               :body    (form-encode body)
               :as      :json}}))

(defmethod command->request :refresh-access-token [{:keys [refresh-token config]}]
  (let [body {:grant_type    "refresh_token"
              :client_id     (-> config :config :client-id)
              :redirect_uri  (get-redirect-uri config)
              :refresh_token refresh-token
              :access_type   "offline"}]
    {:method  :post
     :path    "/oauth2/token"
     :options {:headers {:content-type "application/x-www-form-urlencoded"}
               :body    (form-encode body)
               :as      :json}}))

(defmethod command->request :price-history [{:keys [params symbol]}]
  (let [{:keys [period-type
                periods
                frequency-type
                frequency
                start-date
                end-date]} params
        query-params (-> {:periodType    (name period-type)
                          :frequencyType (name frequency-type)
                          :frequency     frequency}
                         (conj (when-not (and start-date end-date) [:period periods]))
                         (?assoc :startDate start-date)
                         (?assoc :endDate end-date))]
    {:method  :get
     :path    (str "/marketdata/" symbol "/pricehistory")
     :options {:query-params query-params
               :as           :json}}))

(defmethod execute-command :sign-in [{:keys [td-brokerage config] :as command}]
  (->> (command->request command)
       make-request
       :body
       format-sign-in-response
       (save-access-info! td-brokerage (-> config :config :keystore-pass)))
  (load-access-info td-brokerage (-> config :config :keystore-pass)))

(defmethod execute-command :refresh-access-token [{:keys [td-brokerage config] :as command}]
  (let [{:keys [refresh-token]} (load-access-info td-brokerage (-> config :config :keystore-pass))]
    (try
      (->> (assoc command :refresh-token refresh-token)
           command->request
           make-request
           :body
           format-sign-in-response
           (save-access-info! td-brokerage (-> config :config :keystore-pass)))
      (catch Exception e
        (println "Refresh access token failed" e)))
    (load-access-info td-brokerage (-> config :config :keystore-pass))))

(defmethod execute-command :auth-status [{:keys [td-brokerage config]}]
  (load-access-info td-brokerage (-> config :config :keystore-pass)))

(defmethod execute-command :sign-out [{:keys [td-brokerage config]}]
  (doall (map #(.deleteEntry (:keystore td-brokerage) (name %))
              [:access-token :refresh-token :expires-at :refresh-expires-at]))
  (secrets/save-keystore! (:keystore td-brokerage) (-> config :config :keystore-pass) secrets-file)
  (load-access-info td-brokerage (-> config :config :keystore-pass)))

(defmethod execute-command :price-history [{:keys [td-brokerage config] :as command}]
  (map (fn [symbol]
         (->> (assoc command :symbol symbol)
              command->request
              (make-authenticated-request td-brokerage config)
              :body
              format-price-history
              calc-stats))
       (-> config :user-settings deref :symbols)))

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
                                               (get-redirect-uri config)
                                               "&client_id="
                                               (get-in config [:config :client-id])
                                               "%40AMER.OAUTHAP")
                               :keystore  (load-or-create-keystore (get-in config [:config :keystore-pass]))}))

  (stop [this]
    (assoc this :td-brokerage nil)))

(defn make-td []
  (map->TDBrokerage {}))
