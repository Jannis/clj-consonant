(ns clj-consonant.local-store
  (:require [com.stuartsierra.component :as component]
            [clj-consonant.git.repo :as repository]
            [clj-consonant.git.coerce :refer [to-git-ref-name]]
            [clj-consonant.git.commit :as commit]
            [clj-consonant.git.reference :as reference]
            [clj-consonant.actions :refer [ITransaction actions]]
            [clj-consonant.classes :as classes]
            [clj-consonant.objects :as objects]
            [clj-consonant.store :refer :all]
            [clj-consonant.transaction :as transaction]))

;;;; Local store implementation

(defrecord LocalStore [location repo]
  Store
  (connect [this]
    (assoc this :repo (repository/load (:location this))))

  (disconnect [this]
    (assoc this :repo nil))

  (get-refs [this]
    (when (:repo this)
      (reference/load-all (:repo this))))

  (get-ref [this ref-name]
    (when (:repo this)
      (reference/load (:repo this) (to-git-ref-name ref-name))))

  (get-classes [this]
    (get-classes this "HEAD"))

  (get-classes [this ref-name]
    (when (:repo this)
      (classes/load-all (:repo this)
                        (->> (get-ref this ref-name)
                             :head
                             (commit/tree (:repo this))))))

  (get-class [this class-name]
    (get-class this "HEAD" class-name))

  (get-class [this ref-name class-name]
    (when (:repo this)
      (classes/load (:repo this)
                    (->> (get-ref this ref-name)
                         :head
                         (commit/tree (:repo this)))
                    class-name)))

  (get-objects [this class-name]
    (get-objects this "HEAD" class-name))

  (get-objects [this ref-name class-name]
    (when (:repo this)
      (objects/load-all (:repo this)
                        (->> (get-ref this ref-name)
                             :head
                             (commit/tree (:repo this)))
                        (get-class this ref-name class-name))))

  (get-object [this class-name uuid]
    (get-object this "HEAD" class-name uuid))

  (get-object [this ref-name class-name uuid]
    (when (:repo this)
      (objects/load (:repo this)
                    (->> (get-ref this ref-name)
                         :head
                         (commit/tree (:repo this)))
                    (get-class this ref-name class-name)
                    uuid)))

  (get-properties [this class-name uuid]
    (get-properties this "HEAD" class-name uuid))

  (get-properties [this ref-name class-name uuid]
    (when (:repo this)
      (:properties (get-object this ref-name class-name uuid))))

  (get-property [this class-name uuid name]
    (get-property this "HEAD" class-name uuid name))

  (get-property [this ref-name class-name uuid name]
    (when (:repo this)
      (let [object (get-object this ref-name class-name uuid)]
        (or (get-in object [:properties name])
            (get-in object [:properties (keyword name)])))))

  (transact! [this ta]
    {:pre [(satisfies? ITransaction ta)]}
    (when (:repo this)
      (transaction/run! this (actions ta))))

  component/Lifecycle
  (start [component]
    (connect component))
  (stop [component]
    (disconnect component)))

(defn new-local-store [location]
  (LocalStore. location nil))

(defn local-store [location]
  (-> (new-local-store location)
      (connect)))
