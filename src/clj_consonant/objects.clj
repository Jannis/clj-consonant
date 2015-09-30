(ns clj-consonant.objects
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.blob :as blob]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.tree :as git-tree]
            [clj-consonant.classes :as classes]
            [clj-consonant.transit :refer [transit-read]]))

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
