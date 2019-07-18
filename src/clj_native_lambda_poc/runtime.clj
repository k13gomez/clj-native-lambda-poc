(ns clj-native-lambda-poc.runtime
  (:require [clj-http.lite.client :as client]))

(defn get-env
  [var-name]
  (System/getenv (name var-name)))

(def base-url
  (delay (format "http://%s/2018-06-01" (get-env :AWS_LAMBDA_RUNTIME_API))))

(def invocation-next-url
  (delay (format "%s/runtime/invocation/next" @base-url)))

(def invocation-response-url
  (delay (format "%s/runtime/invocation/response" @base-url)))

(def invocation-error-url
  (delay (format "%s/runtime/invocation/error" @base-url)))

(def initialize-error-url
  (delay (format "%s/runtime/init/error" @base-url)))

(defn handle-next-request! 
  []
  (let [next-result (client/get @invocation-next-url {:socket-timeout 0 
                                                     :connection-timeout 0})]
    next-result))
