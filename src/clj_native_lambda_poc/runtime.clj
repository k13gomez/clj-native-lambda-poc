(ns clj-native-lambda-poc.runtime
  (:require [clj-http.lite.client :as client]
            [cheshire.core :as json]
            [clojure.set :refer [rename-keys]])
  (:import [java.lang.reflect Field]
           [java.util Map]))

(def ^:private mutable-env
  (delay
    (let [env (System/getenv)
          ^Class cls (class env)
          ^Field fld (.getDeclaredField cls "m")]
      (.setAccessible fld true)
      (let [^Map m-env (.get fld env)]
        m-env))))

(defn- get-env
  [^String var-name]
  (System/getenv var-name))

(defn- set-env
  [^String var-name ^String env-value]
  (let [^Map env @mutable-env]
    (.put env var-name env-value)))

(def ^:private runtime-api
  (delay (get-env "AWS_LAMBDA_RUNTIME_API")))

(def ^:private handler
  (delay (get-env "_HANDLER")))

(def ^:private root
  (delay (get-env "LAMBDA_TASK_ROOT")))

(def ^:private context-map
  {"lambda-runtime-aws-request-id"        :aws-request-id
   "lambda-runtime-deadline-ms"           :deadline
   "lambda-runtime-invoked-function-arn"  :function-arn
   "lambda-runtime-trace-id"              :trace-id
   "lambda-runtime-client-context"        :client-context
   "lambda-runtime-cognito-identity"      :cognito-identity})

(def ^:private context-keys
  (keys context-map))

(def ^:private invocation-next-url 
  (delay (format "http://%s/2018-06-01/runtime/invocation/next" @runtime-api)))

(defn- next-request!
  []
  (let [next-url @invocation-next-url
        {:keys [body headers]} (client/get next-url {:socket-timeout 0 :conn-timeout 0})
        input (json/parse-string body)
        context (-> (select-keys headers context-keys)
                    (rename-keys context-map)
                    (assoc :handler @handler
                           :root @root))]
    {:input input
     :context context}))

(defn- post-success!
  [{:keys [aws-request-id]} response]
  (let [success-url (format "http://%s/2018-06-01/runtime/invocation/%s/response" @runtime-api aws-request-id)]
    (->> (if-not (string? response) 
           (json/generate-string response)
           response)
         (assoc {} :body) 
         (client/post success-url))))

(defn- ->error-response
  [^Throwable failure]
  {:errorMessage  (.getMessage failure)
   :errorType     (str (type failure))})

(defn- init-failure!
  [failure]
  (println failure)
  (let [failure-url (format "http://%s/2018-06-01/runtime/init/error" @runtime-api)]
    (->> (->error-response failure)
         (json/generate-string)
         (assoc {} :body)
         (client/post failure-url))))

(defn- post-failure!
  [{:keys [aws-request-id]} failure]
  (println failure)
  (let [failure-url (format "http://%s/2018-06-01/runtime/invocation/%s/error" @runtime-api aws-request-id)]
    (->> (->error-response failure)
         (json/generate-string)
         (assoc {} :body)
         (client/post failure-url))))

(defn- handler-name-equals 
  [handler-name handler]
  {:pre [(string? handler-name) (var? handler)]}
  (let [a-meta (meta handler)
        a-ns   (-> a-meta :ns str)
        a-fn   (-> a-meta :name str)
        a-name (str a-ns "/" a-fn)] 
    (= handler-name a-name)))

(defn resolve-handler
  "resolve the current handler from a list of handlers"
  [& handlers]
  {:pre [(every? var? handlers)]}
  (let [handler-name @handler
        handler-predicate (partial handler-name-equals handler-name)
        [request-handler] (filter handler-predicate handlers)]
    (when-not request-handler
      (let [failure (ex-info (format "unable to resolve lambda request handler: %s" handler-name) 
                             {:handler handler-name})]
        (init-failure! failure)
        (throw failure)))
    request-handler))

(defn- handle-next-request!
  [request-handler & {:keys [initialize]}]
  (try
    (let [{:keys [input context]} (next-request!)
          {:keys [trace-id]} context]
      (set-env "_X_AMZN_TRACE_ID" trace-id) ;;; propagate tracing header
      (try
        (let [response (request-handler input context)]
          (post-success! context response))
        (catch Throwable failure
          (post-failure! context failure))))
    (catch Exception failure
      (println failure)
      (when initialize
        (init-failure! failure)))))

(defn run-lambda!
  "run the specified lambda request-handler in a loop"
  [request-handler]
  (handle-next-request! request-handler :initialize true)
  (while true
    (handle-next-request! request-handler)))

(defn start!
  "starts the runtime with the specified lambda function handlers"
  [& handlers]
  (-> (apply resolve-handler handlers)
      (run-lambda!)))
