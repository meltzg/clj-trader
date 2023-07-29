(ns clj-trader.auth
  (:require [rum.core :as rum]))

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
     [:button "Sign In"])])