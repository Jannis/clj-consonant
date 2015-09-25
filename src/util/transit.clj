(ns util.transit
  (:require [cognitect.transit :as transit])
  (:import [java.io ByteArrayOutputStream]))

(defn transit-write
  [data]
  (let [stream (ByteArrayOutputStream. 4096)]
    (-> stream
        (transit/writer :json)
        (transit/write data))
    (.toString stream)))
