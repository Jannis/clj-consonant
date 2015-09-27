(ns test.clj-consonant.local-store
  (:use clojure.test)
  (:require [clojure.pprint :refer [pprint]]
            [environ.core :refer [env]]
            [clj-consonant.store :refer :all]
            [clj-consonant.local-store :refer [local-store]]))

(def test-dir   (:test-dir env))
(def mock-cache {})

(deftest local-store-location-only
  (let [s (local-store test-dir)]
    (is (satisfies? Store s))
    (is (= test-dir (:location s))
    (is (= nil (:cache s))))))

(deftest local-store-with-cache
  (let [s (local-store test-dir mock-cache)]
    (is (satisfies? Store s))
    (is (= test-dir (:location s))
    (is (= mock-cache (:cache s))))))

(deftest local-store-has-repo
  (let [s (local-store test-dir)]
    (is (not (nil? (:repo s))))))

(deftest local-store-has-refs
  (let [s  (local-store test-dir)
        rs (get-refs s)]
    (is (map? rs))
    ; Contains HEAD
    (is (contains? rs "HEAD"))
    (let [r    (get rs "HEAD")
          head (:head r)]
      (is (= "branch" (:type r)))
      (is (map? head))
      (is (= 40 (count (:sha1 head))))
      (is (= "Jannis Pohlmann <jannis@xfce.org>" (:author head))))
    ; Contains master
    (is (contains? rs "refs/heads/master"))
    (let [r    (get rs "refs/heads/master")
          head (:head r)]
      (is (= "branch" (:type r)))
      (is (map? head))
      (is (= 40 (count (:sha1 head)))))
    ; Contains an annotated tag
    (is (contains? rs "refs/tags/annotated-tag"))
    (let [r    (get rs "refs/tags/annotated-tag")
          head (:head r)
          tag  (:tag r)]
      (is (= "tag" (:type r)))
      (is (map? head))
      (is (= 40 (count (:sha1 head))))
      (is (map? tag))
      (is (= 40 (count (:sha1 tag)))))))

(deftest local-store-has-head-ref
  (let [s (local-store test-dir)
        r (get-ref s "HEAD")]
    (is (map? r))
    (is (= "branch" (:type r)))
    (is (= 1 (count (:url-aliases r))))
    (is (some #{"HEAD"} (:url-aliases r)))
    (let [head (:head r)]
      (is (map? head))
      (is (= 40 (count (:sha1 head))))
      (is (= "Jannis Pohlmann <jannis@xfce.org>" (:author head)))
      (is (= "Jannis Pohlmann <jannis@xfce.org>" (:committer head)))
      (is (= "First commit" (:subject head))))))

(deftest local-store-has-master-ref
  (let [s (local-store test-dir)
        r (get-ref s "refs/heads/master")]
    (is (map? r))
    (is (= "branch" (:type r)))
    (is (= 1 (count (:url-aliases r))))
    (is (some #{"refs:heads:master"} (:url-aliases r)))
    (let [head (:head r)]
      (is (map? head))
      (is (= 40 (count (:sha1 head))))
      (is (= "Jannis Pohlmann <jannis@xfce.org>" (:author head)))
      (is (= "Jannis Pohlmann <jannis@xfce.org>" (:committer head)))
      (is (= "First commit" (:subject head))))))
