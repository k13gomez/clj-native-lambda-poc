(ns clj-native-lambda-poc.example
  (:require [clj-http.lite.client :as client]
            [clojure.java.jdbc :as jdbc])
  (:import [software.amazon.awssdk.http.urlconnection UrlConnectionHttpClient]
           [software.amazon.awssdk.auth.credentials EnvironmentVariableCredentialsProvider]
           [software.amazon.awssdk.services.dynamodb DynamoDbClient DynamoDbClientBuilder]
           [software.amazon.awssdk.services.dynamodb.model ListTablesResponse]
           [software.amazon.awssdk.services.s3 S3Client S3ClientBuilder]
           [software.amazon.awssdk.services.s3.model ListBucketsResponse Bucket]
           [org.postgresql.ds PGSimpleDataSource]))

(def http-client
  (delay
    (UrlConnectionHttpClient/create)))

(def creds-provider
  (delay
    (EnvironmentVariableCredentialsProvider/create)))

(def dynamodb-client
  (delay 
    (let [^DynamoDbClientBuilder builder (DynamoDbClient/builder)
          ^DynamoDbClientBuilder with-http (.httpClient builder @http-client)
          ^DynamoDbClientBuilder with-cred (.credentialsProvider with-http @creds-provider)]
      (.build with-cred))))

(def s3-client
  (delay
    (let [^S3ClientBuilder builder (S3Client/builder)
          ^S3ClientBuilder with-http (.httpClient builder @http-client)
          ^S3ClientBuilder with-cred (.credentialsProvider with-http @creds-provider)]
      (.build with-cred))))

(def psqldb-client
  (delay
    (let [^String db-uri (System/getenv "DB_URI")
          ^String db-user (System/getenv "DB_USER")
          ^String db-pass (System/getenv "DB_PASS")
          ^PGSimpleDataSource ds-pool (new PGSimpleDataSource)]
      (.setUrl ds-pool db-uri)
      (.setUser ds-pool db-user)
      (.setPassword ds-pool db-pass)
      ds-pool)))

;; uses rest-json protocol
(defn ^{:init #(deref dynamodb-client)} dynamodb-handler
  [input context]
  (let [^DynamoDbClient client @dynamodb-client
        ^ListTablesResponse response (.listTables client)]
    (.tableNames response)))

;; uses rest-xml protocol
(defn ^{:init #(deref s3-client)} s3-handler 
  [input context]
  (let [^S3Client client @s3-client
        ^ListBucketsResponse response (.listBuckets client)]
    (for [^Bucket bucket (.buckets response)]
      (.name bucket))))

(defn ^{:init #(deref psqldb-client)} sql-handler
  []
  (jdbc/with-db-connection [conn {:datasource @psqldb-client}]
    (let [rows (jdbc/query conn "SELECT 0 AS num")]
      rows)))

(defn request-error-handler
  []
  (throw (ex-info "request handler error." {})))

(defn ^{:init #(throw (ex-info "initialization error." {}))} init-error-handler
  [])

(defn http-handler
  [input context]
  (-> (client/get "https://api.ipify.org?format=json")
      (get :body)))

(defn echo-handler
  [input]
  input)
