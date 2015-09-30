(ns clj-consonant.transaction)

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
