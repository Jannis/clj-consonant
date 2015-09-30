(ns clj-consonant.git.tag
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.ident :as ident]
            [clj-consonant.git.repo :refer [rev-walk]]))

(defrecord Tag [sha1 tagger subject message])

(defn to-tag [repo jtag]
  (let [sha1    (.getName (.getId jtag))
        tagger  (ident/load (.getTaggerIdent jtag))
        subject (.getShortMessage jtag)
        message (.getFullMessage jtag)]
    (->Tag sha1 tagger subject message)))

(defn load [repo oid]
  (->> (.parseTag (rev-walk repo) oid)
       (to-tag repo)))
