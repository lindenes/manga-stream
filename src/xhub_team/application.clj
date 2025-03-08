(ns xhub-team.application
  (:require [clojure.tools.logging :as log]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.params :refer [wrap-params]]
            [xhub-team.domain :as domain])
  (:use xhub-team.logger))

(defn error->respone [error]
  (log/error error)
  (let [data (ex-data error)
        msg  (:error-message data)
        code (:error-code data)]
    (condp = code
      1
      {:status 400 :body msg}

      {:status 500 :body "Unexpected server error"})))

(defn wrap-rest-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e (error->respone e)))))

(defn wrap-cors [handler]
  (fn [req]
    (if (= (:request-method req) :options)
      {:status 200
       :headers {"Access-Control-Allow-Origin" "*"
                 "Access-Control-Allow-Methods" "*"
                 "Access-Control-Allow-Headers" "*"}}
     (let [response ((wrap-multipart-params (wrap-params (wrap-rest-error handler))) req)]
        (-> response
            (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
            (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
            (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization"))))))

(defn is-int [x]
  (try
    (number? (Integer/parseInt x))
    (catch Exception _ false)
    ))

(def validate-error
  {:error-code 1 :error-message "Не верный формат запроса"})

(defn handler [req]
  (log/info "Request:" (:uri req) (:request-method req) )
  (let [uri (:uri req)
        method (:request-method req)
        body (:body req)]
    (cond

      (and (= method :get) (= uri "/manga"))
      (let [params (:query-params req)
            oid (try
                  (Integer/parseInt (get params "oid"))
                  (catch Exception e (throw (ex-info (.getMessage e) validate-error ))))]
        {:status 200 :body (domain/get-manga-page oid)}
        )

      (and (= method :post) (= uri "/manga"))
      (let [params (:multipart-params req)
            filtered-map (filter is-int (keys params))]
        (doseq [elem filtered-map]
          (domain/add-manga-page (:tempfile (get params elem)) (get params "id")))
        {:status 200 :body "OK"})

      :else
      {:status 404 :body "Not Found"})
    )
  )
