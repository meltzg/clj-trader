(ns clj-trader.core
  (:require
    [clj-trader.api-client :as api]
    [clj-trader.auth :refer [authenticator]]
    [clj-trader.user-settings :refer [settings-panel]]
    [cljs.core.async :refer [<! go]]
    [goog.dom :as gdom]
    [rum.core :as rum]))


(defonce app-state (atom {:signed-in?   false}))

(defn handle-auth-change [signed-in?]
  (swap! app-state assoc :signed-in? signed-in?))

(rum/defc content < rum/reactive []
  [:div.sidebar {}
   (authenticator (:signed-in? (rum/react app-state)) handle-auth-change)
   (settings-panel)])

(defn mount [el]
  (rum/mount (content) el))

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount-app-element []
  (when (not-empty (.-search (.-location js/window)))
    (go
      (let [code (-> js/window
                     (.. -location -search)
                     js/URLSearchParams.
                     (.get "code"))
            sign-in-resp (<! (api/sign-in code))]
        (prn sign-in-resp)
        (.replace (.-location js/window)
                  (.-origin (.-location js/window))))))
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element))

