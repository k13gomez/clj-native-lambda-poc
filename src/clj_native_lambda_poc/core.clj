(ns clj-native-lambda-poc.core
  (:require [clj-native-lambda-poc.runtime :as runtime]
            [clj-native-lambda-poc.example :as example])
  (:gen-class))

(defn -main
  [& args]
  (runtime/start!
    #'example/echo-handler
    #'example/dynamodb-handler
    #'example/s3-handler
    #'example/http-handler))
