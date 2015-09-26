#!/usr/bin/env boot

(set-env!
  :source-paths #{"src" "test"}
  :resource-paths #{}
  :dependencies '[; Boot
                  [adzerk/boot-test "1.0.4"]
                  [boot-environ "1.0.1"]

                  ; General
                  [com.cognitect/transit-clj "0.8.281"]
                  [environ "1.0.1"]

                  ; Utilities
                  [danlentz/clj-uuid "0.1.6"]
                  [clj-jgit "0.8.8"]
                  [me.raynes/fs "1.4.6"]])

(task-options!
  pom {:project 'clj-consonant
       :version "0.1.0-SNAPSHOT"})

(require '[adzerk.boot-test :refer :all]
         '[environ.boot :refer [environ]]
         '[environ.core :refer [env]])

; (require '[clj-consonant.store :as store])
;
; (println store)

(deftask create-test-store
  "Create and populate test store"
  []
  (require 'util.create-test-store)
  (comp
    (with-pre-wrap fileset
      ((resolve 'util.create-test-store/run) (:test-dir env))
      fileset)))
