(ns clj-consonant.test.local-store
  (:require [clojure.set :refer [union]]
            [clojure.test :refer [is]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [com.stuartsierra.component :as component]
            [me.raynes.fs :as fs]
            [clj-consonant.actions :as a]
            [clj-consonant.store :as s]
            [clj-consonant.local-store :as ls]
            [clj-consonant.test.setup :as setup :refer [with-store]]))

(defspec new-local-store-not-connected-initially 5
  (prop/for-all [path setup/gen-git-repo]
    (with-store (ls/new-local-store path)
      (is (and (satisfies? s/Store store)
               (satisfies? component/Lifecycle store)
               (nil? (:repo store)))))))

(defspec new-local-store-connected-after-start 5
  (prop/for-all [path setup/gen-git-repo]
    (with-store (ls/new-local-store path)
      (is (not (nil? (-> store component/start :repo)))))))

(defspec new-local-store-disconnected-after-stop 5
  (prop/for-all [path setup/gen-git-repo]
    (with-store (ls/new-local-store path)
      (is (nil? (-> store
                    component/start
                    component/stop
                    :repo))))))

(defspec local-store-already-connected 5
  (prop/for-all [path setup/gen-git-repo]
    (with-store (ls/local-store path)
      (is (not (nil? (:repo store)))))))

(defspec fresh-store-has-no-refs-classes-or-objects 5
  (prop/for-all [store setup/gen-store]
    (with-store store
      (and (is (empty? (s/get-refs store)))
           (is (empty? (s/get-classes store)))))))

(defspec fresh-store-has-an-empty-head-ref 5
  (prop/for-all [store setup/gen-store]
    (with-store store
      (let [ref (s/get-ref store "HEAD")]
        (and (is (not (nil? ref)))
             (is (= "HEAD" (:name ref)))
             (is (= "branch" (:type ref)))
             (is (nil? (:tag ref)))
             (is (nil? (:head ref)))
             (is (= #{:name :type :tag :head}
                    (set (keys ref))))
             (is (empty? (s/get-classes store "HEAD"))))))))

(def gen-transaction-empty
  (gen/fmap (fn [x]
              (-> (a/begin {})
                  (a/commit {:target "HEAD"
                             :author {:name "name" :email "email"}
                             :committer {:name "name" :email "email"}
                             :message "message"})))
            gen/int))

(defspec head-and-master-ref-exist-after-empty-transactions 5
  (prop/for-all [transactions (gen/vector gen-transaction-empty)]
    (with-store (setup/make-store)
      (doseq [ta transactions]
        (s/transact! store ta))
      (let [refs (s/get-refs store)]
        (if (= 0 (count transactions))
          (is (= {} refs))
          (and (is (map? refs))
               (is (= #{"HEAD" "refs:heads:master"}
                      (set (keys refs))))))))))

(defspec head-and-master-are-the-same-after-empty-transactions 5
  (prop/for-all [transactions (gen/vector gen-transaction-empty)]
    (with-store (setup/make-store)
      (doseq [ta transactions]
        (s/transact! store ta))
      (let [head (s/get-ref store "HEAD")
            master (s/get-ref store "refs:heads:master")]
        (is (= (:head head) (:head master)))))))

(defspec empty-transactions-create-no-classes 5
  (prop/for-all [transactions (gen/vector gen-transaction-empty)]
    (with-store (setup/make-store)
      (doseq [ta transactions]
        (s/transact! store ta))
      (let [classes (s/get-classes store "HEAD")]
        (is (empty? classes))))))

(def gen-transaction-create-one
  (gen/fmap (fn [[class properties]]
              (-> (a/begin {})
                  (a/create {:class class
                             :properties properties})
                  (a/commit {:target "HEAD"
                             :author {:name "name" :email "email"}
                             :committer {:name "name" :email "email"}
                             :message "message"})))
            (gen/tuple (gen/not-empty gen/string-alphanumeric)
                       (gen/map gen/keyword gen/int))))

(defn base-on-HEAD [store ta]
  (assoc-in ta [:actions 0 :source]
             (some-> store (s/get-ref "HEAD") :head :sha1)))

(defn get-created-classes [ta]
  (->> ta
       :actions
       (filter #(= :create (a/action-type %)))
       (map :class)
       set))

(defn get-all-created-classes [transactions]
  (reduce union #{} (map get-created-classes transactions)))

(defspec creating-one-object-at-a-time-creates-distinct-objects 20
  (prop/for-all [transactions (gen/vector gen-transaction-create-one)]
    (with-store (setup/make-store)
      (doseq [ta transactions]
        (s/transact! store (base-on-HEAD store ta)))
      (let [input-classes (get-all-created-classes transactions)
            classes (set (keys (s/get-classes store "HEAD")))
            objects (reduce union #{}
                            (map #(s/get-objects store "HEAD" %)
                                 classes))]
        (println (count transactions))
        (and (is (= input-classes classes))
             (is (= (count transactions) (count objects))))))))
