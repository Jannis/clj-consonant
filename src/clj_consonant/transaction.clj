(ns clj-consonant.transaction
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.commit :as git-commit]
            [clj-consonant.git.ident :as ident]
            [clj-consonant.git.reference :as reference]
            [clj-consonant.git.tree :as tree]
            [clj-consonant.actions :refer [IAction action-type]]
            [clj-consonant.classes :as classes]
            [clj-consonant.objects :as objects]))

(defmulti run-action (fn [_ _ _ action] (action-type action)))

(defmethod run-action :begin
  [store actions _ action]
  (if (= (:source action) nil)
      (tree/make-empty (:repo store))
      (->> (git-commit/load (:repo store) (to-oid (:repo store) (:source action)))
           (git-commit/tree (:repo store)))))

(defmethod run-action :create
  [store actions tree action]
  (let [class-name   (:class action)
        uuid         (or (:uuid action) (.toString (java.util.UUID/randomUUID)))
        class-tree   (or (tree/get-tree (:repo store) tree class-name)
                         (tree/make-empty (:repo store)))
        object       (objects/make uuid class-name (:properties action))
        object-blob  (objects/make-blob (:repo store) object)
        object-entry (objects/to-tree-entry object object-blob)]
    (->> object-entry
         (tree/update-entry (:repo store) class-tree)
         (tree/to-tree-entry class-name)
         (tree/update-entry (:repo store) tree))))

(defmethod run-action :delete
  [store actions tree action]
  (let [uuid         (:uuid action)
        class        (classes/load-for-uuid (:repo store) tree uuid)
        class-tree   (tree/get-tree (:repo store) tree (:name class))
        object-entry (first (filter #(= (:name %) uuid) (:entries class-tree)))]
    (if-not (nil? object-entry)
      (->> object-entry
        (tree/remove-entry (:repo store) class-tree)
        (tree/to-tree-entry (:name class))
        (tree/update-entry (:repo store) tree))
      tree)))

(defmethod run-action :update
  [store actions tree action]
  (let [uuid         (:uuid action)
        class        (classes/load-for-uuid (:repo store) tree uuid)
        class-tree   (tree/get-tree (:repo store) tree (:name class))
        object       (objects/make uuid (:name class) (:properties action))
        object-blob  (objects/make-blob (:repo store) object)
        object-entry (objects/to-tree-entry object object-blob)]
    (->> object-entry
         (tree/update-entry (:repo store) class-tree)
         (tree/to-tree-entry (:name class))
         (tree/update-entry (:repo store) tree))))

(defmethod run-action :commit
  [store actions tree action]
  (let [begin   (first actions)
        source  (when (:source begin) (to-oid (:repo store) (:source begin)))
        parent  (when source (git-commit/load (:repo store) source))
        parents (if parent [parent] [])
        commit  (git-commit/make (:repo store)
                                 tree
                                 parents
                                 :author (ident/from-map (:author action))
                                 :committer (ident/from-map (:committer action))
                                 :message (:message action))
        target  (reference/load (:repo store) (:target action))]
    (when target
      (when (reference/update! (:repo store) target commit)
        (let [updated-ref (reference/load (:repo store) (:target action))]
          updated-ref)))))

(defn run! [store actions]
  (when store
    (reduce (partial run-action store actions) nil actions)))
