#!/usr/bin/env boot

(set-env!
  :source-paths #{"test"}
  :resource-paths #{"src"}
  :dependencies '[; Boot
                  [adzerk/boot-cljs "1.7.170-1"]
                  [adzerk/boot-test "1.0.5" :scope "test"]
                  [boot-environ "1.0.1"]
                  [pandeiro/boot-http "0.6.3"]

                  ; Testing
                  [org.clojure/test.check "0.9.0" :scope "test"]
                  [me.raynes/fs "1.4.6" :scope "test"]

                  ; General
                  [clj-jgit "0.8.8"]
                  [clj-time "0.11.0"]
                  [com.cognitect/transit-clj "0.8.281"]
                  [com.stuartsierra/component "0.3.0"]
                  [compojure "1.4.0"]
                  [danlentz/clj-uuid "0.1.6"]
                  [environ "1.0.1"]
                  [ring/ring-core "1.4.0"]
                  [ring-middleware-format "0.6.0"]
                  [ring.middleware.logger "0.5.0"]

                  ; Client
                  [org.clojure/clojurescript "1.7.170"]
                  [cljs-ajax "0.5.1"]

                  ; Utilities
                  [me.raynes/fs "1.4.6"]])

(task-options!
  pom {:project 'clj-consonant
       :version "0.1.0-SNAPSHOT"})

(require '[adzerk.boot-cljs :refer :all]
         '[adzerk.boot-test :refer :all]
         '[environ.boot :refer [environ]]
         '[environ.core :refer [env]]
         '[pandeiro.boot-http :refer [serve]])

(deftask service
  "Launch a Consonant service"
  [s store PATH str "Consonant store to serve"]
  (comp
    (with-pre-wrap fileset
      (System/setProperty "consonant-service-store" store)
      fileset)
    (serve :port 8080
           :httpkit true
           :reload true
           :handler 'clj-consonant.service/service)
    (watch)
    (speak)))

(deftask deploy
  "Deploy to Maven"
  []
  (comp
    (pom)
    (jar)
    (install)))
