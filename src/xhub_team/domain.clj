(ns xhub-team.domain
  (:require [xhub-team.infrastructure :as infra]
             [clojure.java.io :as io]))

(defn add-manga-page [^java.io.File page ^String manga-id]
  (infra/save-large-object-from-file page manga-id))

(defn get-manga-page [^Integer oid]
  (infra/read-large-object oid))
