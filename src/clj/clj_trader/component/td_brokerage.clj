(ns clj-trader.component.td-brokerage
  (:require [clj-http.client :as http]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-trader.component.config :as config]
            [clj-trader.utils.helpers :refer [?assoc]]
            [clj-trader.utils.secrets :as secrets]
            [clojure.java.io :as io]
            [clojure.tools.logging :as logger]
            [com.stuartsierra.component :as component]
            [ring.util.codec :refer [form-encode]]))

(def api-root "https://api.tdameritrade.com/v1")
(def secrets-file "td-secrets.jks")

(def period-frequency-info {:valid-periods                   {:day   [1 2 3 4 5 10]
                                                              :month [1 2 3 6]
                                                              :year  [1 2 3 5 10 15 20]
                                                              :ytd   [1]}
                            :valid-frequency-type-for-period {:day   [:minute]
                                                              :month [:daily :weekly]
                                                              :year  [:daily :weekly :monthly]
                                                              :ytd   [:daily :weekly]}
                            :valid-frequencies               {:minute  [1 5 10 15 30]
                                                              :daily   [1]
                                                              :weekly  [1]
                                                              :monthly [1]}})

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
  {:ticker        symbol
   :price-candles (map #(select-keys % [:open :high :low :close :datetime]) candles)})

(defn- calc-stat [price-candles keys stat-prefix f]
  (into {} (map (fn [key]
                  [(keyword (str stat-prefix (name key)))
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
    (logger/log :info (str "make brokerage request " (update-in request [:headers :authorization] some?)))
    (http/request request)))

(defn- make-authenticated-request
  ([td-brokerage config-manager request]
   (make-authenticated-request td-brokerage config-manager request true))
  ([td-brokerage config-manager request refresh-tokens?]
   (let [{:keys [access-token]} (load-access-info td-brokerage (:keystore-pass (config/get-app-settings config-manager)))]
     (try
       (make-request (assoc-in request [:options :headers :authorization]
                               (str "Bearer " access-token)))
       (catch Exception e
         (if (and refresh-tokens? (= 401 (-> e ex-data :status)))
           (do
             (logger/log :warn "refresh token and retry")
             (execute-command {:command        :refresh-access-token
                               :td-brokerage   td-brokerage
                               :config-manager config-manager})
             (make-authenticated-request td-brokerage config-manager request false))
           (throw e)))))))

(defmethod command->request :sign-in [{:keys [code config-manager]}]
  (let [body {:grant_type   "authorization_code"
              :access_type  "offline"
              :code         code
              :client_id    (:client-id (config/get-app-settings config-manager))
              :redirect_uri (config/get-redirect-uri config-manager)}]
    {:method  :post
     :path    "/oauth2/token"
     :options {:headers {:content-type "application/x-www-form-urlencoded"}
               :body    (form-encode body)
               :as      :json}}))

(defmethod command->request :refresh-access-token [{:keys [refresh-token config-manager]}]
  (let [body {:grant_type    "refresh_token"
              :client_id     (:client-id (config/get-app-settings config-manager))
              :redirect_uri  (config/get-redirect-uri config-manager)
              :refresh_token refresh-token
              :access_type   "offline"}]
    {:method  :post
     :path    "/oauth2/token"
     :options {:headers {:content-type "application/x-www-form-urlencoded"}
               :body    (form-encode body)
               :as      :json}}))

(defmethod command->request :price-history [{:keys [params ticker]}]
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
     :path    (str "/marketdata/" ticker "/pricehistory")
     :options {:query-params query-params
               :as           :json}}))

(defmethod execute-command :sign-in [{:keys [td-brokerage config-manager] :as command}]
  (->> (command->request command)
       make-request
       :body
       format-sign-in-response
       (save-access-info! td-brokerage (:keystore-pass (config/get-app-settings config-manager))))
  (load-access-info td-brokerage (:keystore-pass (config/get-app-settings config-manager))))

(defmethod execute-command :refresh-access-token [{:keys [td-brokerage config-manager] :as command}]
  (let [{:keys [refresh-token]} (load-access-info td-brokerage (:keystore-pass (config/get-app-settings config-manager)))]
    (when refresh-token
      (try
        (->> (assoc command :refresh-token refresh-token)
             command->request
             make-request
             :body
             format-sign-in-response
             (save-access-info! td-brokerage (:keystore-pass (config/get-app-settings config-manager))))
        (catch Exception e
          (logger/log :error e "Refresh access token failed"))))
    (load-access-info td-brokerage (:keystore-pass (config/get-app-settings config-manager)))))

(defmethod execute-command :auth-status [{:keys [td-brokerage config-manager]}]
  (load-access-info td-brokerage (:keystore-pass (config/get-app-settings config-manager))))

(defmethod execute-command :sign-out [{:keys [td-brokerage config-manager]}]
  (doall (map #(.deleteEntry (:keystore td-brokerage) (name %))
              [:access-token :refresh-token :expires-at :refresh-expires-at]))
  (secrets/save-keystore! (:keystore td-brokerage) (:keystore-pass (config/get-app-settings config-manager)) secrets-file)
  (load-access-info td-brokerage (:keystore-pass (config/get-app-settings config-manager))))

(defmethod execute-command :price-history [{:keys [td-brokerage config-manager params] :as command}]
  (let [tickers (or (:tickers params)
                    (:tickers (config/get-user-settings config-manager)))]
    (pmap (fn [ticker]
            (->> (assoc command :ticker ticker)
                 command->request
                 (make-authenticated-request td-brokerage config-manager)
                 :body
                 format-price-history
                 calc-stats))
          (if (string? tickers)
            [tickers]
            tickers))))

(defn load-or-create-keystore [keystore-pass]
  (if (.exists (io/file secrets-file))
    (secrets/load-keystore! secrets-file keystore-pass)
    (let [ks (secrets/make-new-keystore keystore-pass)]
      (secrets/save-keystore! ks keystore-pass secrets-file)
      ks)))

(defrecord TDBrokerage [config-manager]
  component/Lifecycle

  (start [this]
    (assoc this :td-brokerage {:oauth-uri (str "https://auth.tdameritrade.com/auth?response_type=code&redirect_uri="
                                               (config/get-redirect-uri config-manager)
                                               "&client_id="
                                               (:client-id (config/get-app-settings config-manager))
                                               "%40AMER.OAUTHAP")
                               :keystore  (load-or-create-keystore (:keystore-pass (config/get-app-settings config-manager)))}))

  (stop [this]
    (assoc this :td-brokerage nil)))

(defn make-td []
  (map->TDBrokerage {}))
