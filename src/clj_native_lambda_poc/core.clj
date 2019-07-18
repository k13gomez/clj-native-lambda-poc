(ns clj-native-lambda-poc.core
  (:require [clj-native-lambda-poc.runtime :as runtime]
            [clj-native-lambda-poc.example :as example])
  (:gen-class))

(defn -main
  [& args]
  (while true
    (runtime/handle-next-request!
      #'example/echo-handler)))
