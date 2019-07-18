(ns clj-native-lambda-poc.core
  (:require [clj-native-lambda-poc.runtime :as runtime])
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (runtime/handle-next-request!))
