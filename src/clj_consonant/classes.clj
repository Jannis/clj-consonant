(ns clj-consonant.classes
  (:refer-clojure :exclude [load])
  (:require [clj-consonant.debug :refer [print-and-return]]
            [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.tree :as git-tree]))

(defrecord ConsonantClass [name objects])

(defn load-from-entry [repo entry]
  (let [name    (:name entry)
        oid     (to-oid repo (:sha1 entry))
        tree    (git-tree/load repo oid)
        objects (map (fn [e] {:uuid (:name e)}) (:entries tree))]
    (->ConsonantClass name objects)))

(defn load-all [repo tree]
  ; (println "classes/load-all" repo tree)
  (->>
      ;  (print-and-return "> tree" tree)
       (:entries tree)
      ;  (print-and-return "> tree entries")
       (filter #(= :tree (:type %)))
      ;  (print-and-return "> subtree entries")
       (map (partial load-from-entry repo))
      ;  (print-and-return "> classes")
       (map #(-> [(:name %) %]))
      ;  (print-and-return "> with names")
       (into {})))

(defn load [repo tree name]
  (-> (load-all repo tree)
      (get name)))

(defn tree [repo tree class]
  (->> (:entries tree)
       (filter #(= (:name class) (:name %)))
       (first)
       :sha1
       (to-oid repo)
       (git-tree/load repo)))

(defn load-for-uuid [repo tree uuid]
  (println "classes/load-for-uuid" tree uuid)
  (->> (load-all repo tree)
       (print-and-return "> classes")
       (vals)
       (print-and-return "> values")
       (filter #(some #{{:uuid uuid}} (:objects %)))
       (print-and-return "> matching classes")
       (first)))
