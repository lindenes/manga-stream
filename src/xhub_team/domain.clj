(ns xhub-team.domain
  (:require [xhub-team.infrastructure :as infra]
             [clojure.java.io :as io]))

(defn add-manga-page [^java.io.File page ^String manga-id]
  (infra/save-photo page (java.util.UUID/fromString manga-id)))

(defn get-manga-page [manga-id page-id]
  (infra/read-photo manga-id page-id))
