(ns xhub-team.infrastructure
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clojure.java.io :as io]
            [xhub-team.configuration :as conf]
            [amazonica.aws.s3 :as s3]
            [xhub-team.errors :as err])
  (:import [com.zaxxer.hikari HikariDataSource HikariConfig]
           (org.postgresql.largeobject LargeObjectManager)
           (java.io File FileInputStream)
           (java.io ByteArrayOutputStream)
           (org.postgresql PGConnection)))

(def datasource
  (let [config (HikariConfig.)
        database-config (:database conf/config)]
    (.setJdbcUrl config (:url database-config))
    (.setUsername config (:user database-config))
    (.setPassword config (:password database-config))

    (.setMaximumPoolSize config 10)
    (.setMinimumIdle config 2)
    (.setIdleTimeout config 30000)
    (.setMaxLifetime config 1800000)

    (HikariDataSource. config)))

(defn save-photo [file manga-id]
  (let [photo-id (java.util.UUID/randomUUID)]
     (try
       (s3/put-object conf/aws-creds
               :bucket-name "hentai-page-bucket"
               :key (str manga-id ":" photo-id)
               :file file )
    (jdbc/with-transaction [tx datasource]
      (sql/insert! tx :manga_page {:id photo-id :manga_id manga-id}))
    (catch Exception e (throw (ex-info (.getMessage e) err/photo-load-error))))))

(defn read-photo [manga-id photo-id]
  (let [s3-photo  (s3/get-object conf/aws-creds
                                 :bucket-name "hentai-page-bucket"
                                 :key (str manga-id ":" photo-id))]
     (:object-content s3-photo)))
