(ns clj-consonant.git.tree
  (:import [org.eclipse.jgit.lib FileMode TreeFormatter])
  (:refer-clojure :exclude [load update])
  (:require [clj-consonant.git.coerce :refer [to-oid to-sha1]]
            [clj-consonant.git.repo :refer [object-inserter
                                            rev-walk
                                            tree-walk
                                            tree-walk-for-entry]]))

(defn tree-formatter []
  (TreeFormatter.))

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

(defn to-jtree [repo tree]
  (->> (:sha1 tree)
       (to-oid repo)
       (.parseTree (rev-walk repo))))

(defn load [repo oid]
  (->> (.parseTree (rev-walk repo) oid)
       (to-tree repo)))

(defn make-empty [repo]
  (let [formatter (tree-formatter)
        inserter  (object-inserter repo)
        oid       (.insert inserter formatter)]
    (.flush inserter)
    (load repo oid)))

(defn get-tree [repo tree subtree-name]
  (let [jtree (to-jtree repo tree)
        walk  (tree-walk-for-entry repo jtree subtree-name)]
    (when walk
      (->> (.getObectId walk 0)
           (load repo)))))

(defn contains-entry? [tree name]
  ((complement empty?) (filter #{name} (map :name (:entries tree)))))

(defn update [repo tree entry]
  (println "update" tree entry)
  (let [formatter (tree-formatter)]
    (doseq [cur (:entries tree)]
      (if (= (:entry name) (.getPathString cur))
        (.append formatter (:name entry) (:type entry) (to-oid repo (:sha1 entry)))
        (.append formatter (:name cur) (:type cur) (to-oid repo (:sha1 cur)))))
    (when-not (contains-entry? tree (:name entry))
      (.append formatter (:name entry) (:type entry) (to-oid repo (:sha1 entry))))
    (let [inserter (object-inserter repo)
          oid      (.insert inserter formatter)]
      (.flush inserter)
      (load repo oid))))

(defn to-tree-entry [name tree]
  (->TreeEntry name (:sha1 tree) FileMode/TREE))
