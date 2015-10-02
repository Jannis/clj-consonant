(ns clj-consonant.transaction
  (:refer-clojure :exclude [run!])
  (:require [clojure.string :as str]
            [clj-consonant.git.coerce :refer [to-oid]]
            [clj-consonant.git.commit :as git-commit]
            [clj-consonant.git.ident :as ident]
            [clj-consonant.git.reference :as reference]
            [clj-consonant.git.tree :as tree]
            [clj-consonant.classes :as classes]
            [clj-consonant.objects :as objects]))

;;;; JGit helpers

; (defn- make-tree
;   ([store]
;    (make-tree store []))
;   ([store objects]
;    (let [formatter (TreeFormatter.)]
;      (doseq [object objects]
;        (.append formatter (:name object) (:type object) (:oid object)))
;      (let [inserter  (object-inserter store)
;            oid       (.insert inserter formatter)]
;        (.flush inserter)
;        (.lookupTree (rev-walk store) oid)))))
;
; (defn- make-identity
;   ([s] (PersonIdent. "Foo" "bar@bar.org")))
;
; (defn- make-commit
;   [store tree & {:keys [author committer message]}]
;   (println tree author committer message)
;   (let [repo (:repo store)
;         builder (CommitBuilder.)]
;     (.setTreeId builder (.getId tree))
;     (.setAuthor builder author)
;     (.setCommitter builder committer)
;     (.setMessage builder message)
;     (let [inserter (git-object-inserter repo)
;           oid      (.insert inserter builder)]
;       (.flush inserter)
;       (.parseCommit (RevWalk. (.getRepository repo)) oid))))
;
; (defn- update-ref
;   [store refname commit]
;   (let [update (.updateRef (.getRepository (:repo store)) refname)]
;     (.setNewObjectId update (.getId commit))
;     (.update update)))
;
; ;;;; Transactions
;
; (defmulti apply-action (fn [_ _ action] (:action action)))
;
; (defmethod apply-action :begin
;   [store _ action]
;   (println "apply-action" :begin action)
;   (if (= (:source action) (str/join (repeat 40 "0")))
;     (make-tree store)
;     (->> (to-oid store (:source action))
;          (.parseCommit (rev-walk store))
;          (.getTree))))
;
; (defmethod apply-action :create
;   [store tree action]
;   (println "apply-action" :create action)
;   (println "tree" tree)
;   tree)
;
; (defmethod apply-action :end
;   [store tree action]
;   (println "apply-action" :end action)
;   (println "tree" tree)
;   (->> (make-commit store tree
;                     :author    (make-identity (:author action))
;                     :committer (make-identity (:committer action))
;                     :message   (:message action))
;        (update-ref store (:target action)))
;   (->> (load-ref store (:target action))
;        (parse-ref store)))

(defmulti run-action (fn [_ _ _ action] (:action action)))

(defmethod run-action :begin
  [store actions _ action]
  (println "run-action" :begin)
  (if (= (:source action) (str/join (repeat 40 "0")))
      (tree/make-empty (:repo store))
      (->> (git-commit/load (:repo store) (to-oid (:source action)))
           (git-commit/tree (:repo store)))))

(defmethod run-action :create
  [store actions tree action]
  (println "run-action" :create tree)
  (let [class-name   (:class action)
        class-tree   (or (tree/get-tree (:repo store) tree class-name)
                         (tree/make-empty (:repo store)))
        _            (println "class-tree" class-tree)
        object       (objects/make class-name (:properties action))
        _            (println "object" object)
        object-blob  (objects/make-blob (:repo store) object)
        _            (println "object-blob" object-blob)
        object-entry (objects/to-tree-entry object object-blob)]
    (->> object-entry
         (tree/update (:repo store) class-tree)
         (tree/to-tree-entry class-name)
         (tree/update (:repo store) tree))))

(defmethod run-action :commit
  [store actions tree action]
  (println "run-action" :commit tree)
  (let [begin   (first actions)
        source  (when-not (= (:source begin) (str/join (repeat 40 "0")))
                  (to-oid (:repo store) (:source begin)))
        parent  (when source
                  (git-commit/load (:repo store) source))
        parents (if parent [parent] [])
        commit  (git-commit/make (:repo store)
                                 tree
                                 parents
                                 :author (ident/from-map (:author action))
                                 :committer (ident/from-map (:committer action))
                                 :message (:message action))
        target  (reference/load (:repo store) (:target action))]
    (println "COMMIT" commit)
    (println "TARGET" target)
    nil))

(defn run! [store actions]
  (when store
    (reduce (partial run-action store actions) nil actions)))
