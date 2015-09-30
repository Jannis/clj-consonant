(ns clj-consonant.local-store
  (:require [clj-jgit.porcelain :refer [load-repo]]
            [clj-consonant.git.coerce :refer [to-refname]]
            [clj-consonant.git.reference :as reference]
            [clj-consonant.classes :as classes]
            [clj-consonant.objects :as objects]
            [clj-consonant.store :refer :all]))

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

;;;; Local store implementation

(defrecord LocalStore [location cache repo]
  Store

  (connect [this]
    (assoc this :repo (load-repo (:location this))))

  (disconnect [this]
    (assoc this :repo nil))

  (get-refs [this]
    (when (:repo this)
      (reference/load-all (:repo this))))

  (get-ref [this ref-alias]
    (when (:repo this)
      (reference/load (:repo this) (to-refname ref-alias))))

  (get-classes [this]
    (get-classes this "HEAD"))

  (get-classes [this ref-alias]
    (when (:repo this)
      (->> (get-ref this ref-alias)
           :head
           (classes/load-all (:repo this)))))

  (get-class [this class-name]
    (get-class this "HEAD" class-name))

  (get-class [this ref-alias class-name]
    (when (:repo this)
      (classes/load (:repo this)
                    (:head (get-ref this ref-alias))
                    class-name)))

  (get-objects [this class-name]
    (get-objects this "HEAD" class-name))

  (get-objects [this ref-alias class-name]
    (when (:repo this)
      (objects/load-all (:repo this)
                        (:head (get-ref this ref-alias))
                        (get-class this ref-alias class-name))))

  (get-object [this class-name uuid]
    (get-object this "HEAD" class-name uuid))

  (get-object [this ref-alias class-name uuid]
    (when (:repo this)
      (objects/load (:repo this)
                    (:head (get-ref this ref-alias))
                    (get-class this ref-alias class-name)
                    uuid)))

  (get-properties [this class-name uuid]
    (get-properties this "HEAD" class-name uuid))

  (get-properties [this ref-alias class-name uuid]
    (when (:repo this)
      (:properties (get-object this ref-alias class-name uuid))))

  (get-property [this class-name uuid name]
    (get-property this "HEAD" class-name uuid name))

  (get-property [this ref-alias class-name uuid name]
    (when (:repo this)
      (let [object (get-object this ref-alias class-name uuid)]
        (or (get-in object [:properties name])
            (get-in object [:properties (keyword name)])))))

  (transact! [this actions]
    ))
    ; (when (:repo this)
    ;   (reduce (partial apply-action this) :start actions))))

(defn local-store
  ([location]
    (-> (LocalStore. location nil nil)
        (connect)))
  ([location cache]
    (-> (LocalStore. location cache nil)
        (connect))))
