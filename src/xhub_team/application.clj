(ns xhub-team.application
  (:require [clojure.tools.logging :as log]
             [xhub-team.errors :as err]
              [clojure.data.json :as json]
            [xhub-team.domain :as domain])
  (:use xhub-team.logger)
  (:import (java.io File FileInputStream)))

(defn error->response [error]
  (let [data (ex-data error)
        error-data (:error-data data)]
    (log/error data)
    (let [error-map  (condp = (:error_code (first error-data))
                        1
                        {:status 400 :body error-data }

                        2
                        {:status 503 :body error-data}

                        {:status 500 :body "Unexpected server error"}) ]
         (-> error-map
             (assoc :body (json/write-str (:body error-map)))
             (assoc-in [:headers "Content-Type"] "application/json")))))

(defn wrap-rest-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e (error->response e)))))

(defn wrap-cors [handler]
  (fn [req]
    (if (= (:request-method req) :options)
      {:status 200
       :headers {"Access-Control-Allow-Origin" "*"
                 "Access-Control-Allow-Methods" "*"
                 "Access-Control-Allow-Headers" "*"}}
     (let [response (handler req)]
        (-> response
            (assoc-in [:headers "Access-Control-Allow-Origin"] "*")
            (assoc-in [:headers "Access-Control-Allow-Methods"] "GET, POST, PUT, DELETE, OPTIONS")
            (assoc-in [:headers "Access-Control-Allow-Headers"] "Content-Type, Authorization"))))))

(defn is-int [x]
  (try
    (number? (Integer/parseInt x))
    (catch Exception _ false)
    ))

(defn handler [req]
  (log/info "Request:" (:uri req) (:request-method req) )
  (log/info (:multipart-params req))
  (let [uri (:uri req)
        method (:request-method req)
        body (:body req)]
    (cond
      (and (= method :get) (= uri "/manga"))
      (let [params (:query-params req)
            manga-id (get params "manga_id")
            page-id (get params "page_id")
            file (domain/get-manga-page manga-id page-id)]
        {:status 200 :body file})

      (and (= method :post) (= uri "/manga"))
      (let [params (:multipart-params req)
            filtered-map (filter is-int (keys params))
            uuid (try (java.util.UUID/fromString (get params "id"))
                 (catch Exception e (throw (ex-info (.getMessage e) err/validate-error ))))]
        (doseq [elem filtered-map]
          (domain/add-manga-page (:tempfile (get params elem)) uuid))
        {:status 200 :body "OK"})

      :else
      {:status 404 :body "Not Found"})
    )
  )
