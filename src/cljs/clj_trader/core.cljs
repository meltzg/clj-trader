(ns clj-trader.core
  (:require
    [ajax.core :as ajax]
    [clj-trader.auth :refer [authenticator, handle-refresh]]
    [clj-trader.price-history :refer [price-history]]
    [clj-trader.user-settings :refer [settings-panel]]
    [clj-trader.utils :refer [api-url]]
    [goog.dom :as gdom]
    ["@mui/icons-material/Menu$default" :as MenuIcon]
    ["@mui/material" :refer [AppBar
                             Box
                             Toolbar
                             IconButton
                             Typography]]
    [rum.core :as rum]))


(defonce app-state (atom {:signed-in?    false
                          :user-settings nil}))

(defn handle-auth-change [signed-in?]
  (swap! app-state assoc :signed-in? signed-in?))

(defn handle-user-settings-change [user-settings]
  (swap! app-state assoc :user-settings user-settings))

(defn initialize-auth-mixin []
  {:will-mount
   (fn [state]
     (handle-refresh (:signed-in? @app-state) handle-auth-change false)
     state)})

(rum/defc content < rum/reactive (initialize-auth-mixin) []
  [:> Box {:sx {:display  "flex"
                :flexGrow 1}}
   [:> AppBar {:position "static"}
    [:> Toolbar
     [:> IconButton {:size       "large"
                     :edge       "start"
                     :color      "inherit"
                     :aria-label "menu"
                     :sx         {:mr 2}}
      [:> MenuIcon {}]]
     [:> Typography {:variant "h6" :component "div" :sx {:flexGrow 1}}
      "CLJ-Trader"]
     (authenticator (:signed-in? (rum/react app-state)) handle-auth-change)]]
   ;[:div.horizontal
   ; [:div.sidebar
   ;  (authenticator (:signed-in? (rum/react app-state)) handle-auth-change)
   ;  (settings-panel (:user-settings (rum/react app-state)) handle-user-settings-change)]
   ; [:div.mainview
   ;  (price-history)]]
   ])

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

