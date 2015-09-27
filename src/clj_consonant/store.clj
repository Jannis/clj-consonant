(ns clj-consonant.store)

(defprotocol Store
  "Standard interface to access local and remote Consonant stores."
  (connect [this] "Connect to a store")
  (disconnect [this] "Disconnect from a store")
  (get-refs [this] "Return a map of all refs in the store")
  (get-ref [this alias] "Return the ref for the given alias"))
