(ns xhub-team.manga-stream
  (:require [org.httpkit.server :as hk-server])
  (:use xhub-team.application)
  (:gen-class))

(def my-server (hk-server/run-server handler {:port 8080}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
   (hk-server/run-server handler {:port 8080}))

(my-server)
