(ns clj-consonant.git.blob
  (:import [org.eclipse.jgit.lib Constants])
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.coerce :refer [to-sha1]]
            [clj-consonant.git.repo :refer [object-inserter object-loader]]
            [clj-consonant.transit :refer [transit-read]]))

(defrecord Blob [sha1 data])

(defn load [repo oid]
  (->> (object-loader repo oid)
       (.getBytes)
       (->Blob (to-sha1 oid))))

(defn write [repo data]
  (let [inserter (object-inserter repo)
        oid      (.insert inserter (Constants/OBJ_BLOB) data)]
    (.flush inserter)
    (->Blob (to-sha1 oid) data)))
