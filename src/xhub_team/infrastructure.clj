(ns xhub-team.infrastructure
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [clojure.java.io :as io]
            [xhub-team.configuration :as conf])
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

(defn save-large-object-from-file [file manga-id]
  (jdbc/with-transaction [tx datasource]
    (let [conn (.unwrap tx PGConnection)
          _ (.setAutoCommit conn false)
          lob-manager (.getLargeObjectAPI conn)
          oid (.createLO lob-manager)]
      (with-open [lob (.open lob-manager oid LargeObjectManager/WRITE)
                  input-stream (FileInputStream. file)]
        (io/copy input-stream (.getOutputStream lob) :buffer-size 65536)
        (sql/insert! tx :manga_page {:oid oid :manga_id manga-id} )))))

(defn read-large-object [oid]
  (jdbc/with-transaction [tx datasource]
    (let [conn (.unwrap tx PGConnection)
          lob-manager (.getLargeObjectAPI conn)]
      (with-open [ lob (.open lob-manager oid LargeObjectManager/READ)
                  output-stream (ByteArrayOutputStream.)]
        (io/copy (.getInputStream lob) output-stream :buffer-size 65535)
        (.toByteArray output-stream)))))
