(ns clj-trader.auth
  (:require [rum.core :as rum]
            [cljs.core.async :refer [<! go]]
            [clj-trader.api-client :as api]))

(defn initiate-auth []
  (go
    (let [oauth-uri (<! (api/get-oauth-uri))]
      (.replace (.-location js/window) oauth-uri))))

(rum/defc auth-status [signed-in?]
  (if signed-in?
    [:div.signin-status.signed-in "Connected"]
    [:div.signin-status.signed-out "Disconnected"]))

(rum/defc authenticator [signed-in? change-signed-in]
  [:div.authenticator
   (auth-status signed-in?)
   (if signed-in?
     [:div
      [:button "Refresh Status"]
      [:button "Sign Out"]]
     [:button {:on-click initiate-auth} "Sign In"])])
