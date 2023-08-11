(ns clj-trader.mui-extension
  (:require [cljs-material-ui.core :as mui]))

(defn stack [& args] (mui/create-mui-el "Stack" args))
