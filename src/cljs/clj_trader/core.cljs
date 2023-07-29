(ns clj-trader.core
  (:require
    [goog.dom :as gdom]
    [rum.core :as rum]
    [jayq.core :refer [$]]
    [cljs.core.async :refer [<! go]]
    [clj-trader.auth :refer [authenticator]]
    [clj-trader.api-client :as api]))


(defonce app-state (atom {:counter      0
                          :echo-content nil}))

(defn handle-counter-click []
  (prn @app-state)
  (swap! app-state update :counter inc))

(defn handle-echo-submit []
  (go
    (let [message (.val ($ "#echo-text"))
          echo-message (<! (api/do-echo message))]
      (prn echo-message)
      (swap! app-state assoc :echo-content echo-message))))

(rum/defc counter [number]
  [:div {:on-click handle-counter-click}
   (str "Clicked " number " times")])

(rum/defc echo [message]
  [:div
   [:h3 message]
   [:div
    [:label
     "Message"
     [:input {:type "text" :id "echo-text"}]]
    [:input {:type "submit" :value "Submit" :on-click handle-echo-submit}]]])

(rum/defc content < rum/reactive []
  [:div {}
   (authenticator true #())
   (counter (:counter (rum/react app-state)))
   (echo (:echo-content (rum/react app-state)))])

(defn mount [el]
  (rum/mount (content) el))

(defn get-app-element []
  (gdom/getElement "app"))

(defn mount-app-element []
  (when-let [el (get-app-element)]
    (mount el)))

;; conditionally start your application based on the presence of an "app" element
;; this is particularly helpful for testing this ns without launching the app
(mount-app-element)

;; specify reload hook with ^:after-load metadata
(defn ^:after-load on-reload []
  (mount-app-element)
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  )
