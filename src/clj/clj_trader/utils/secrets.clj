(ns clj-trader.utils.secrets
  (:import (java.io FileInputStream FileOutputStream)
           (java.security KeyStore KeyStore$PasswordProtection KeyStore$SecretKeyEntry)
           (javax.crypto SecretKeyFactory)
           (javax.crypto.spec PBEKeySpec)))

(defn make-new-keystore [keystore-password]
  (let [ks (KeyStore/getInstance "JCEKS")]
    (.load ks nil (.toCharArray keystore-password))
    ks))

(defn set-secret-in-keystore! [ks keystore-password entry entry-secret]
  (let [factory (SecretKeyFactory/getInstance "PBE")
        generated-secret (.generateSecret factory (PBEKeySpec. (.toCharArray entry-secret)))
        keystore-pp (KeyStore$PasswordProtection. (.toCharArray keystore-password))]
    (.load ks nil (.toCharArray keystore-password))
    (.setEntry ks (name entry) (KeyStore$SecretKeyEntry. generated-secret) keystore-pp)))

(defn get-secret-from-keystore [ks keystore-password entry]
  (let [keystore-pp (KeyStore$PasswordProtection. (.toCharArray keystore-password))
        factory (SecretKeyFactory/getInstance "PBE")
        ske (.getEntry ks (name entry) keystore-pp)
        key-spec (.getKeySpec factory (.getSecretKey ske) PBEKeySpec)
        password (.getPassword key-spec)]
    (String. password)))

(defn save-keystore! [ks keystore-password path]
  (with-open [f-out (FileOutputStream. path)]
    (.store ks f-out (.toCharArray keystore-password))))

(defn load-keystore! [path keystore-password]
  (with-open [f-in (FileInputStream. path)]
    (let [ks (KeyStore/getInstance "JCEKS")]
      (.load ks f-in (.toCharArray keystore-password))
      ks)))
