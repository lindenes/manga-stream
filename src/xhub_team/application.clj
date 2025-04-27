(ns xhub-team.application
  (:require [clojure.tools.logging :as log]
            [xhub-team.errors :as err]
            [clojure.data.json :as json]
            [xhub-team.infrastructure :as infra])
  (:use xhub-team.logger)
  (:import (java.io File FileInputStream)))

(defn error->response [error]
  (let [data (ex-data error)
        error-data (:error-data data)]
    (log/error error)
    (let [error-map  (condp = (:error_code (first error-data))
                       1
                       {:status 400 :body error-data}

                       2
                       {:status 503 :body error-data}

                       3
                       {:status 404 :body error-data}

                       4
                       {:status 401 :body error-data}

                       5
                       {:status 403 :body error-data}

                       6
                       {:status 404 :body (:error-data err/validate-error)}

                       {:status 500 :body "Unexpected server error"})]
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
    (catch Exception _ false)))

(defn handler [req]
  (log/info "Request:" (:uri req) (:request-method req))
  (log/info (:multipart-params req))
  (let [uri (:uri req)
        token (-> req :headers (get "token"))
        method (:request-method req)
        body (:body req)]
    (cond
      (and (= method :get) (= uri "/manga"))
      (let [params (:query-params req)
            manga-id (get params "manga_id")
            page-id (get params "page_id")
            file (infra/read-photo manga-id page-id)]
        {:status 200 :body file})

      (and (= method :post) (= uri "/manga"))
      (let [params (:multipart-params req)
            id (get params "id")
            have-privileges (infra/check-privileges token id)
            filtered-map (sort (filter is-int (keys params)))]
        (when (nil? token) (throw (ex-info "Token not found" err/not-auth-user)))
        (when-not have-privileges (throw (ex-info "User hase not priveleges" err/load-delete-permission-error)))
        (doseq [elem filtered-map]
          (infra/save-photo (:tempfile (get params elem)) id))
        {:status 200 :body "OK"})

      (and (= method :delete) (= uri "/manga"))
      (let [params (:query-params req)
            manga-id (get params "manga_id")
            have-privileges (infra/check-privileges token manga-id)]
        (when (nil? token) (throw (ex-info "Token not found" err/not-auth-user)))
        (when-not have-privileges (throw (ex-info "User hase not priveleges" err/load-delete-permission-error)))
        (infra/delete-photos manga-id)
        {:status 200 :body "OK"})

      :else
      {:status 404 :body "Not Found"})))
