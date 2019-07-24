(ns clj-native-lambda-poc.example
  (:require [clj-http.lite.client :as client])
  (:import [software.amazon.awssdk.http SdkHttpClient]
           [software.amazon.awssdk.http.urlconnection UrlConnectionHttpClient]
           [software.amazon.awssdk.auth.credentials EnvironmentVariableCredentialsProvider]
           [software.amazon.awssdk.services.dynamodb DynamoDbClient DynamoDbClientBuilder]
           [software.amazon.awssdk.services.dynamodb.model ListTablesResponse]
           [software.amazon.awssdk.services.s3 S3Client S3ClientBuilder]
           [software.amazon.awssdk.services.s3.model ListBucketsResponse Bucket]))

(def ^SdkHttpClient http-client
  (UrlConnectionHttpClient/create))

(def dynamodb-client
  (atom nil))

(def s3-client
  (atom nil))

(defn initialize! []
  (let [^EnvironmentVariableCredentialsProvider cred-provider (EnvironmentVariableCredentialsProvider/create)
        dynamodb-client* (let [^DynamoDbClientBuilder builder (DynamoDbClient/builder)
                               ^DynamoDbClientBuilder with-http (.httpClient builder http-client)
                               ^DynamoDbClientBuilder with-cred (.credentialsProvider with-http cred-provider)]
                           (.build with-cred))
        s3-client* (let [^S3ClientBuilder builder (S3Client/builder)
                         ^S3ClientBuilder with-http (.httpClient builder http-client)
                         ^S3ClientBuilder with-cred (.credentialsProvider with-http cred-provider)]
                     (.build with-cred))]
    (reset! dynamodb-client dynamodb-client*)
    (reset! s3-client s3-client*)))

;; uses rest-json protocol
(defn dynamodb-handler
  [input context]
  (let [^DynamoDbClient client @dynamodb-client
        ^ListTablesResponse response (.listTables client)]
    (.tableNames response)))

;; uses rest-xml protocol
(defn s3-handler
  [input context]
  (let [^S3Client client @s3-client
        ^ListBucketsResponse response (.listBuckets client)]
    (for [^Bucket bucket (.buckets response)]
      (.name bucket))))

(defn http-handler
  [input context]
  (-> (client/get "https://api.ipify.org?format=json")
    (get :body)))

(defn echo-handler
  [input context]
  input)
