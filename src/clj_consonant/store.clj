(ns clj-consonant.store)

(defprotocol Store
  "Standard interface to access local and remote Consonant stores."
  (connect [this] "Connect to a store")
  (disconnect [this] "Disconnect from a store")
  (refs [this] "Return a map of all refs in the store"))
