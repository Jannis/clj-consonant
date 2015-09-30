(ns clj-consonant.git.tree
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.repo :refer [rev-walk tree-walk]]))

(defrecord TreeEntry [name sha1 type])
(defrecord Tree [sha1 entries])

(defn entries [repo jtree]
  (let [walk    (tree-walk repo [jtree])
        entries (transient [])]
    (while (.next walk)
      (conj! entries (->TreeEntry (.getPathString walk)
                                  (.getName (.getObjectId walk 0))
                                  (if (.isSubtree walk) :tree :file))))
    (persistent! entries)))

(defn to-tree [repo jtree]
  (let [sha1    (.getName jtree)
        entries (entries repo jtree)]
    (->Tree sha1 entries)))

(defn load [repo oid]
  (->> (.parseTree (rev-walk repo) oid)
       (to-tree repo)))
