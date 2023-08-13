(ns clj-trader.auth
  (:require [ajax.core :as ajax]
            [clj-trader.mui-extension :as mui-x]
            [clj-trader.utils :refer [api-url]]
            [cljs-material-ui.core :as mui]
            [rum.core :as rum]))

(defn initiate-auth []
  (ajax/GET (str api-url "oauthUri")
            {:response-format :json
             :keywords?       true
             :handler         (fn [{:keys [oauth-uri]}]
                                (.replace (.-location js/window) oauth-uri))}))

(defn refresh-auth-status [current-state on-change]
  (ajax/GET (str api-url "authStatus")
            {:response-format :json
             :keywords?       true
             :handler         (fn [{:keys [signed-in?]}]
                                (when-not (= current-state signed-in?)
                                  (on-change signed-in?)))}))

(rum/defc auth-status [signed-in?]
  (if signed-in?
    [:div.signin-status.signed-in "Connected"]
    [:div.signin-status.signed-out "Disconnected"]))

(rum/defc authenticator [signed-in? change-signed-in]
  (refresh-auth-status signed-in? change-signed-in)
  [:div.authenticator
   (auth-status signed-in?)
   (mui-x/stack {:direction "row" :spacing 0.5}
                (mui/button {:variant  "contained"
                             :on-click #(refresh-auth-status signed-in? change-signed-in)}
                            "Refresh Status")
                (if signed-in?
                  (mui/button {:variant  "contained"
                               :on-click (fn []
                                           (ajax/GET (str api-url "signOut")
                                                     {:handler #(refresh-auth-status signed-in? change-signed-in)}))}
                              "Sign Out")
                  (mui/button {:variant  "contained"
                               :on-click initiate-auth}
                              "Sign In")))])
