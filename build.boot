#!/usr/bin/env boot

(set-env!
  :source-paths #{"src"}
  :resource-paths #{}
  :dependencies '[; General
                  [com.cognitect/transit-clj "0.8.281"]

                  ; Utilities
                  [danlentz/clj-uuid "0.1.6"]
                  [clj-jgit "0.8.8"]
                  [me.raynes/fs "1.4.6"]])

(task-options!
  pom {:project 'clj-consonant
       :version "0.1.0-SNAPSHOT"})

(deftask create-test-store
  "Create and populate test store"
  []
  (require 'util.create-test-store)
  (comp
    (with-pre-wrap fileset
      ((resolve 'util.create-test-store/run))
      fileset)))
