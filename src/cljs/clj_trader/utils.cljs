(ns clj-trader.utils)

(def api-url (str (first (string/split (.-href (.-location js/window))
                                       #"\?"))
                  "api/"))
