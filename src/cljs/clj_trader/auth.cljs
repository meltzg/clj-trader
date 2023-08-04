(ns clj-trader.auth
  (:require [clj-trader.api-client :as api]
            [cljs.core.async :refer [<! go]]
            [rum.core :as rum]))

(defn initiate-auth []
  (go
    (let [oauth-uri (<! (api/get-oauth-uri))]
      (.replace (.-location js/window) oauth-uri))))

(defn refresh-auth-status [current-state on-change]
  (go (let [auth-status (<! (api/auth-status))]
        (when-not (= current-state (:signed-in? auth-status))
          (on-change (:signed-in? auth-status))))))

(rum/defc auth-status [signed-in?]
  (if signed-in?
    [:div.signin-status.signed-in "Connected"]
    [:div.signin-status.signed-out "Disconnected"]))

(rum/defc authenticator [signed-in? change-signed-in]
  (refresh-auth-status signed-in? change-signed-in)
  [:div.authenticator
   (auth-status signed-in?)
   (if signed-in?
     [:div
      [:button {:on-click #(refresh-auth-status signed-in? change-signed-in)} "Refresh Status"]
      [:button {:on-click (fn []
                            (go (<! (api/sign-out))
                                (refresh-auth-status signed-in? change-signed-in)))} "Sign Out"]]
     [:button {:on-click initiate-auth} "Sign In"])])
