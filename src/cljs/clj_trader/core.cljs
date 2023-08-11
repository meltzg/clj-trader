(ns clj-trader.core
  (:require
    [ajax.core :as ajax]
    [clj-trader.auth :refer [authenticator]]
    [clj-trader.price-history :refer [price-history]]
    [clj-trader.user-settings :refer [settings-panel]]
    [clj-trader.utils :refer [api-url]]
    [goog.dom :as gdom]
    [rum.core :as rum]))


(defonce app-state (atom {:signed-in?    false
                          :user-settings nil}))

(defn handle-auth-change [signed-in?]
  (swap! app-state assoc :signed-in? signed-in?))

(defn handle-user-settings-change [user-settings]
  (swap! app-state assoc :user-settings user-settings))

(rum/defc content < rum/reactive []
  [:div.horizontal
   [:div.sidebar
    (authenticator (:signed-in? (rum/react app-state)) handle-auth-change)
    (settings-panel (:user-settings (rum/react app-state)) handle-user-settings-change)]
   [:div.mainview
    (price-history)]])

(defn mount [el]
  (rum/mount (content) el))

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount-app-element []
  (when (not-empty (.-search (.-location js/window)))
    (let [code (-> js/window
                   (.. -location -search)
                   js/URLSearchParams.
                   (.get "code"))]
      (ajax/POST (str api-url "signIn")
                 {:params          {:code code}
                  :format          :json
                  :response-format :json
                  :keywords?       true?
                  :handler         (fn [response]
                                     (prn response)
                                     (.replace (.-location js/window)
                                               (.-origin (.-location js/window))))})))
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))

