(ns clj-consonant.test.local-store
  (:require [clojure.test :refer [is]]
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
      (println "STORE" store)
      (println "REPO" (:repo store))
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

(def gen-empty-transaction
  (gen/fmap (fn [x]
              (-> (a/begin {})
                  (a/commit {:target "HEAD"
                             :author {:name "name" :email "email"}
                             :committer {:name "name" :email "email"}
                             :message "message"})))
            gen/int))

(defspec head-and-master-ref-exist-after-empty-transactions 5
  (prop/for-all [transactions (gen/vector gen-empty-transaction)]
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
  (prop/for-all [transactions (gen/vector gen-empty-transaction)]
    (with-store (setup/make-store)
      (doseq [ta transactions]
        (s/transact! store ta))
      (let [head (s/get-ref store "HEAD")
            master (s/get-ref store "refs:heads:master")]
        (is (= (:head head) (:head master)))))))

(defspec empty-transactions-create-no-classes 5
  (prop/for-all [transactions (gen/vector gen-empty-transaction)]
    (with-store (setup/make-store)
      (doseq [ta transactions]
        (s/transact! store ta))
      (let [classes (s/get-classes store "HEAD")]
        (is (empty? classes))))))
