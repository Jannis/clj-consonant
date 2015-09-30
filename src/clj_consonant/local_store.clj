(ns clj-consonant.local-store
  (:require [clj-consonant.git.repo :as repository]
            [clj-consonant.git.coerce :refer [to-refname]]
            [clj-consonant.git.reference :as reference]
            [clj-consonant.classes :as classes]
            [clj-consonant.objects :as objects]
            [clj-consonant.store :refer :all]))

;;;; Local store implementation

(defrecord LocalStore [location cache repo]
  Store

  (connect [this]
    (assoc this :repo (repository/load (:location this))))

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
