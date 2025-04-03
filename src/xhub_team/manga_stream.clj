(ns xhub-team.manga-stream
  (:require [org.httpkit.server :as hk-server]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.params :refer [wrap-params]]
            [xhub-team.configuration :as conf])
  (:use xhub-team.application)
  (:gen-class))

(def my-server (hk-server/run-server (-> handler
                                         wrap-multipart-params
                                         wrap-params
                                         wrap-cors
                                         wrap-rest-error)
                                     (conf/config :application) ))

(defn -main
  [& args]
  (hk-server/run-server (-> handler
                            wrap-multipart-params
                            wrap-params
                            wrap-cors
                            wrap-rest-error)
                        (conf/config :application))
  (println "server started witg port" (conf/config :application)))

(my-server)
