(ns clj-trader.component.td-brokerage
  (:require [com.stuartsierra.component :as component]
            [clj-trader.component.config :refer [get-redirect-uri]])
  (:import (java.io FileInputStream FileOutputStream)
           (java.security KeyStore KeyStore$PasswordProtection KeyStore$SecretKeyEntry)
           (javax.crypto SecretKeyFactory)
           (javax.crypto.spec PBEKeySpec)))

(defn make-new-keystore [entry entry-password keystore-password]
  (let [factory (SecretKeyFactory/getInstance "PBE")
        generated-secret (.generateSecret factory (PBEKeySpec. (.toCharArray entry-password)))
        ks (KeyStore/getInstance "JCEKS")
        keystore-pp (KeyStore$PasswordProtection. (.toCharArray keystore-password))]
    (.load ks nil (.toCharArray keystore-password))
    (.setEntry ks entry (KeyStore$SecretKeyEntry. generated-secret) keystore-pp)
    ks))

(defn get-password-from-keystore [entry ks keystore-password]
  (let [keystore-pp (KeyStore$PasswordProtection. (.toCharArray keystore-password))
        factory (SecretKeyFactory/getInstance "PBE")
        ske (.getEntry ks entry keystore-pp)
        key-spec (.getKeySpec factory (.getSecretKey ske) PBEKeySpec)
        password (.getPassword key-spec)]
    (String. password)))

(defn save-keystore! [ks keystore-password path]
  (.store ks (FileOutputStream. path) (.toCharArray keystore-password)))

(defn load-keystore! [path keystore-password]
  (let [ks (KeyStore/getInstance "JCEKS")
        f-in (FileInputStream. path)]
    (.load ks f-in (.toCharArray keystore-password))
    ks))

(defrecord TDBrokerage [config]
  component/Lifecycle

  (start [this]
    (assoc this :td-brokerage {:oauth-uri (str "https://auth.tdameritrade.com/auth?response_type=code&redirect_uri="
                                               (get-redirect-uri config)
                                               "&client_id="
                                               (get-in config [:config :client-id])
                                               "%40AMER.OAUTHAP")}))

  (stop [this]
    (assoc this :td-brokerage nil)))

(defn make-td []
  (map->TDBrokerage {}))
