(ns clj-consonant.classes
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.tree :as git-tree]))

(defrecord ConsonantClass [name objects])

(defn load-from-entry [repo entry]
  (let [name    (:name entry)
        oid     (to-oid repo (:sha1 entry))
        tree    (git-tree/load repo oid)
        objects (map (fn [e] {:uuid (:name e)}) (:entries tree))]
    (->ConsonantClass name objects)))

(defn load-all [repo commit]
  (->> (commit/tree repo commit)
       :entries
       (filter #(= :tree (:type %)))
       (map (partial load-from-entry repo))
       (map #(-> [(:name %) %]))
       (into {})))

(defn load [repo commit name]
  (-> (load-all repo commit)
      (get name)))

(defn tree [repo commit class]
  (->> (commit/tree repo commit)
       :entries
       (filter #(= (:name class) (:name %)))
       (first)
       :sha1
       (to-oid repo)
       (git-tree/load repo)))
