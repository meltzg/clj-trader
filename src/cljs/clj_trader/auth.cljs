(ns clj-trader.auth
  (:require [ajax.core :as ajax]
            [clj-trader.utils :refer [api-url]]
            ["@mui/icons-material/AccountCircle$default" :as AccountCircle]
            ["@mui/material" :refer [Button
                                     IconButton
                                     Menu
                                     MenuItem
                                     Stack]]
            [rum.core :as rum]))

(def component-state (atom {}))

(defn initiate-auth []
  (ajax/GET (str api-url "oauthUri")
            {:response-format :json
             :keywords?       true
             :handler         (fn [{:keys [oauth-uri]}]
                                (.replace (.-location js/window) oauth-uri))}))

(defn handle-menu [event]
  (swap! component-state assoc :anchor-element (.-currentTarget event)))

(defn handle-close []
  (swap! component-state dissoc :anchor-element))

(defn handle-refresh
  ([current-status on-change]
   (handle-refresh current-status on-change true))
  ([current-state on-change close-menu?]
   (ajax/GET (str api-url "authStatus")
             {:response-format :json
              :keywords?       true
              :handler         (fn [{:keys [signed-in?]}]
                                 (when-not (= current-state signed-in?)
                                   (on-change signed-in?))
                                 (when close-menu? (handle-close)))})))

(defn handle-sign-out [current-state on-change]
  (ajax/GET (str api-url "signOut")
            {:handler #(handle-refresh current-state on-change)}))

(rum/defc authenticator < rum/reactive [signed-in? change-signed-in]
  ;(handle-refresh signed-in? change-signed-in)
  [:div
   [:> IconButton {:size "large"
                   :aria-label "user brokerage connection status"
                   :aria-controls "menu-connection"
                   :aria-haspopup "true"
                   :onClick handle-menu
                   :color (if signed-in? "default" "error")}
    [:> AccountCircle {}]]
   [:> Menu {:id "menu-connection"
             :anchorEl (:anchor-element (rum/react component-state))
             :anchorOrigin {:vertical "top" :horizontal "right"}
             :keepMounted true
             :transformOrigin {:vertical "top" :horizontal "right"}
             :open (some? (:anchor-element (rum/react component-state)))
             :onClose handle-close}
    [:> MenuItem {:onClick #(handle-refresh signed-in? change-signed-in)}
     "Refresh Status"]
    (if signed-in?
      [:> MenuItem {:onClick handle-sign-out}
       "Sign Out"]
      [:> MenuItem {:onClick initiate-auth}
       "Sign In"])]])
