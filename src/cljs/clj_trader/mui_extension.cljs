(ns clj-trader.mui-extension
  (:require [cljs-material-ui.core :as mui]))

(defn stack [& args] (mui/create-mui-el "Stack" args))

(defn table-container [& args] (mui/create-mui-el "TableContainer" args))

(defn table-cell [& args] (mui/create-mui-el "TableCell" args))
