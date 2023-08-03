(ns clj-trader.core
  (:require
    [clj-trader.api-client :as api]
    [clj-trader.auth :refer [authenticator]]
    [cljs.core.async :refer [<! go]]
    [goog.dom :as gdom]
    [rum.core :as rum]))


(defonce app-state (atom {:counter      0
                          :echo-content nil
                          :to-echo      nil
                          :signed-in?   false}))

(defn handle-counter-click []
  (prn @app-state)
  (swap! app-state update :counter inc))

(defn handle-echo-submit [message]
  (go
    (let [echo-message (<! (api/do-echo message))]
      (swap! app-state assoc :echo-content echo-message))))

(defn handle-auth-change [signed-in?]
  (swap! app-state assoc :signed-in? signed-in?))

(rum/defc counter [number]
  [:div {:on-click handle-counter-click}
   (str "Clicked " number " times")])

(rum/defc echo [message]
  (let [[value set-value!] (rum/use-state "")]
    [:div
     [:h3 message]
     [:div
      [:label
       "Message"
       [:input {:type      "text"
                :id        "echo-text"
                :value     value
                :on-change #(set-value! (.. % -target -value))}]]
      [:input {:type "submit" :value "Submit" :on-click #(handle-echo-submit value)}]]]))

(rum/defc content < rum/reactive []
  [:div {}
   (authenticator (:signed-in? (rum/react app-state)) handle-auth-change)
   (counter (:counter (rum/react app-state)))
   (echo (:echo-content (rum/react app-state)))])

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

