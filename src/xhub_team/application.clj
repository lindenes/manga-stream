(ns xhub-team.application
  (:require [clojure.tools.logging :as log])
  (:use xhub-team.logger))

(defn app [req]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    "hello HTTP!"})


(defn handler [req]
  (log/info "Request:" (:uri req) (:request-method req) )
  (let [uri (:uri req)
        method (:request-method req)
        body (:body req)]
    (cond

      (and (= method :get) (= uri "/manga"))
      {:status 404 :body "Not Found"}

      (and (= method :post) (= uri "/manga"))
      {:status 404 :body "Not Found"}

      :else
      {:status 404 :body "Not Found"})
    )
  )
