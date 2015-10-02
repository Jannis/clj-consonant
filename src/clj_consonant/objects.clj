(ns clj-consonant.objects
  (:import [java.util UUID]
           [org.eclipse.jgit.lib FileMode])
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.blob :as blob]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.tree :as git-tree]
            [clj-consonant.classes :as classes]
            [clj-consonant.transit :refer [transit-read transit-write]]))

(defrecord ConsonantObject [uuid class properties])

(defn load-from-entry [repo class entry]
  (let [uuid  (:name entry)
        oid   (to-oid repo (:sha1 entry))
        blob  (blob/load repo oid)
        props (transit-read (:data blob))]
    (->ConsonantObject uuid (:name class) props)))

(defn load-all [repo commit class]
  (->> (classes/tree repo commit class)
       :entries
       (filter #(= :file (:type %)))
       (map (partial load-from-entry repo class))))

(defn load [repo commit class uuid]
  (->> (classes/tree repo commit class)
       :entries
       (filter #(= :file (:type %)))
       (filter #(= uuid (:name %)))
       (first)
       (load-from-entry repo class)))

(defn make [class properties]
  (->ConsonantObject (.toString (UUID/randomUUID)) class properties))

(defn make-blob [repo object]
  (blob/write repo (-> (:properties object)
                       (transit-write)
                       (.getBytes "utf-8"))))

(defn to-tree-entry [object blob]
  (git-tree/->TreeEntry (:uuid object)
                        (:sha1 blob)
                        FileMode/REGULAR_FILE))
