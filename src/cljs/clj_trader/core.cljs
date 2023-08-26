(ns clj-trader.core
  (:require
    [ajax.core :as ajax]
    [clj-trader.auth :refer [authenticator, handle-refresh]]
    [clj-trader.price-history :refer [price-history]]
    [clj-trader.user-settings :refer [settings-panel]]
    [clj-trader.utils :refer [api-url]]
    [goog.dom :as gdom]
    ["@mui/icons-material/Analytics$default" :as AnalyticsIcon]
    ["@mui/icons-material/Menu$default" :as MenuIcon]
    ["@mui/icons-material/Settings$default" :as SettingsIcon]
    ["@mui/material" :refer [AppBar
                             Box
                             Drawer
                             IconButton
                             List
                             ListItem
                             ListItemButton
                             ListItemIcon
                             ListItemText
                             Stack
                             Toolbar
                             Typography]]
    [rum.core :as rum]))


(defonce app-state (atom {:signed-in?   false
                          :show-drawer? false
                          :open-app     :analysis}))

(defn handle-auth-change [signed-in?]
  (swap! app-state assoc :signed-in? signed-in?))

(defn handle-user-settings-change [user-settings]
  (swap! app-state assoc :user-settings user-settings))

(defn initialize-auth-mixin []
  {:will-mount
   (fn [state]
     (handle-refresh (:signed-in? @app-state) handle-auth-change false)
     state)})

(defn toggle-drawer [event]
  (when-not (and (some? event)
                 (= (.-type event) "keydown")
                 (or (= (.-key event) "Tab")
                     (= (.-key event) "Shift")))
    (swap! app-state update :show-drawer? not)))

(defn handle-change-open-app [key]
  (swap! app-state assoc :open-app key)
  (toggle-drawer nil))

(defn render-list-item [text icon key]
  [:> ListItem {:key key}
   [:> ListItemButton {:onClick #(handle-change-open-app key)}
    [:> ListItemIcon
     [:> icon]]
    [:> ListItemText {:primary text}]]])

(rum/defc content < rum/reactive (initialize-auth-mixin) []
  [:> Box {:sx {:display  "flex"
                :flexGrow 1}}
   [:> Stack {:direction "column"}
    [:> AppBar {:position "static"}
     [:> Toolbar
      [:> IconButton {:size       "large"
                      :edge       "start"
                      :color      "inherit"
                      :aria-label "menu"
                      :sx         {:mr 2}
                      :onClick    toggle-drawer}
       [:> MenuIcon {}]]
      [:> Typography {:variant "h6" :component "div" :sx {:flexGrow 1}}
       "CLJ-Trader"]
      (authenticator (:signed-in? (rum/react app-state)) handle-auth-change)]]
    [:> Drawer {:sx      {:drawerWidth                   240
                          :flexShrink                    0
                          (keyword "& .MuiDrawer-paper") {:width     240
                                                          :boxSizing "border-box"}}
                :anchor  "left"
                :open    (:show-drawer? (rum/react app-state))
                :onClose toggle-drawer}
     [:> List
      (map #(apply render-list-item %) [["Analysis" AnalyticsIcon :analysis]
                                        ["Auto-Trader" SettingsIcon :auto-trader]])]]
    [:> Box {:component "main"}
     (case (:open-app (rum/react app-state))
       :analysis (price-history)
       :auto-trader (settings-panel (:user-settings (rum/react app-state)) handle-user-settings-change))]]])

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

