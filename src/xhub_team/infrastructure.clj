(ns xhub-team.infrastructure
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clojure.java.io :as io]
            [xhub-team.configuration :as conf]
            [taoensso.carmine :as car :refer [wcar]]
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

(defonce my-conn-pool (car/connection-pool {}))
(def     my-conn-spec conf/config->redis)
(def     my-wcar-opts {:pool my-conn-pool, :spec my-conn-spec})
(defmacro wcar* [& body] `(car/wcar my-wcar-opts ~@body))

(defn check-privileges [token manga-id]
  (let [manga-id->uuid (try (java.util.UUID/fromString manga-id)
                            (catch Exception e (throw (ex-info (.getMessage e) err/uuid_parse_error))))
        user (wcar* (car/get token))
        author-id (-> (with-open [conn (jdbc/get-connection datasource)
                                  stmt (jdbc/prepare conn ["select author_id from manga where id = ?" manga-id->uuid])]
                        (jdbc/execute! stmt))
                      first
                      :manga/author_id
                      .toString)]
    (when (nil? user) (throw (ex-info "not found user in storage" err/not-auth-user)))
    (= (:id user) author-id)))

(defn save-photo [file manga-id]
  (let [photo-id (java.util.UUID/randomUUID)]
    (try
      (s3/put-object conf/aws-creds
                     :bucket-name "hentai-page-bucket"
                     :key (str manga-id ":" photo-id)
                     :file file)
      (jdbc/with-transaction [tx datasource]
        (sql/insert! tx :manga_page {:id photo-id :manga_id (java.util.UUID/fromString manga-id)}))
      (catch Exception e (throw (ex-info (.getMessage e) err/photo-load-error))))))

(defn read-photo [manga-id photo-id]
  (try
    (let [s3-photo  (s3/get-object conf/aws-creds
                                   :bucket-name "hentai-page-bucket"
                                   :key (str manga-id ":" photo-id))]
      (:object-content s3-photo))
    (catch Exception ex (throw (ex-info (.getMessage ex) err/photo-not-found)))))

(defn manga->pages [manga-id]
  (with-open [conn (jdbc/get-connection datasource)
              stmt (jdbc/prepare conn ["select id from manga_page where manga_id = ?" (java.util.UUID/fromString manga-id)])]
    (mapv
     (fn [x] (.toString (:manga_page/id x)))
     (jdbc/execute! stmt))))

(defn s3-delete-photos [manga-id]
  (let [page-ids (manga->pages manga-id)]
    (when (seq page-ids) (s3/delete-objects conf/aws-creds
                                            {:bucket-name "hentai-page-bucket"
                                             :keys (mapv (fn [x] (str manga-id ":" x))  page-ids)}))))

(defn database-delete-photos [manga-id with-manga]
  (with-open [conn (jdbc/get-connection datasource)]
    (let [uuid (java.util.UUID/fromString manga-id)
          sql-query (if (or (true? with-manga) (= "true" with-manga))
                      ["DELETE FROM manga WHERE id = ?" uuid]
                      ["DELETE FROM manga_page WHERE manga_id = ?" uuid])]
      (with-open [stmt (jdbc/prepare conn sql-query)]
        (jdbc/execute! stmt)))))

(defn delete-photos [manga-id with-manga]
  (s3-delete-photos manga-id)
  (database-delete-photos manga-id with-manga))
