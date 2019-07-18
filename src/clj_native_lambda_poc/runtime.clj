(ns clj-native-lambda-poc.runtime
  (:require [clj-http.lite.client :as client])
  (:import [clojure.lang Var Named Namespace]))

(defn get-env
  [var-name]
  (System/getenv var-name))

(def runtime-api
  (delay (get-env "AWS_LAMBDA_RUNTIME_API")))

(def handler
  (delay (get-env "_HANDLER")))

(def root
  (delay (get-env "LAMBDA_TASK_ROOT")))

(def invocation-next-url
  (memoize 
    (fn [host]
      (format "http://%s/2018-06-01/runtime/invocation/next" host))))

(def initialize-error-url
  (memoize
    (fn [host]
      (format "http://%s/2018-06-01/runtime/init/error" host))))

(defn invocation-response-url
  [host request-id]
  (format "http://%s/2018-06-01/runtime/invocation/%s/response" host request-id))

(defn invocation-error-url
  [host request-id]
  (format "http://%s/2018-06-01/runtime/invocation/%s/error" host request-id))

(defn- handler-name-equals 
  [handler-name handler]
  (let [a-meta (meta handler)
        a-ns   (-> a-meta :ns str)
        a-fn   (-> a-meta :name str)
        a-name (str a-ns "/" a-fn)] 
    (= handler-name a-name)))

(def find-handler
  (memoize
    (fn [handler-name handlers]
      (let [handler-predicate (partial handler-name-equals handler-name)
            [handler] (filter handler-predicate handlers)]
        handler))))

(defn handle-next-request! 
  [& handlers]
  {:pre [(every? var? handlers)]}
  (let [next-url (invocation-next-url @runtime-api)
        request (client/get next-url {:socket-timeout 0 :conn-timeout 0})
        request-id (get-in request [:headers "lambda-runtime-aws-request-id"])
        resp-url (invocation-response-url @runtime-api request-id)
        error-url (invocation-error-url @runtime-api request-id)
        req-handler (find-handler @handler handlers)]
    (client/post resp-url {:body (str (req-handler request))})))
