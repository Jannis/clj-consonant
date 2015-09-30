(ns clj-consonant.git.blob
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.coerce :refer [to-sha1]]
            [clj-consonant.git.repo :refer [object-loader]]
            [clj-consonant.transit :refer [transit-read]]))

(defrecord Blob [sha1 data])

(defn load [repo oid]
  (->> (object-loader repo oid)
       (.getBytes)
       (->Blob (to-sha1 oid))))
