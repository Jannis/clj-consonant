#!/usr/bin/env boot

(set-env!
  :source-paths #{"src" "test"}
  :resource-paths #{}
  :dependencies '[; Boot
                  [adzerk/boot-reload "0.3.1"]
                  [adzerk/boot-test "1.0.4"]
                  [boot-environ "1.0.1"]
                  [pandeiro/boot-http "0.6.3"]

                  ; General
                  [clj-jgit "0.8.8"]
                  [clj-time "0.11.0"]
                  [com.cognitect/transit-clj "0.8.281"]
                  [compojure "1.4.0"]
                  [environ "1.0.1"]

                  ; Utilities
                  [danlentz/clj-uuid "0.1.6"]
                  [me.raynes/fs "1.4.6"]])

(task-options!
  pom {:project 'clj-consonant
       :version "0.1.0-SNAPSHOT"})

(require '[adzerk.boot-reload :refer [reload]]
         '[adzerk.boot-test :refer :all]
         '[environ.boot :refer [environ]]
         '[environ.core :refer [env]]
         '[pandeiro.boot-http :refer [serve]])

(deftask create-test-store
  "Create and populate test store"
  []
  (require 'util.create-test-store)
  (comp
    (with-pre-wrap fileset
      ((resolve 'util.create-test-store/run) (:test-dir env))
      fileset)))

(deftask service
  "Launch a Consonant service"
  []
  (comp
    (serve :port 8080
           :httpkit true
           :init 'clj-consonant.service/start-service
           :cleanup 'clj-consonant.service/stop-service
           :handler 'clj-consonant.service/service)
    (watch)
    (reload)))
