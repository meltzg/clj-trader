(ns clj-trader.core
  (:require
    [goog.dom :as gdom]
    [rum.core :as rum]))

(defonce app-state (atom {:counter 0}))

(defn handle-click []
  (swap! app-state update :counter inc))

(rum/defc counter [number]
  [:div {:on-click handle-click}
   (str "Clicked " number " times")])

(rum/defc content < rum/reactive []
  [:div {}
   (counter (:counter (rum/react app-state)))])

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

