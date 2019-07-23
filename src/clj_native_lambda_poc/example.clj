(ns clj-native-lambda-poc.example
  (:import [software.amazon.awssdk.http.urlconnection UrlConnectionHttpClient]
           [software.amazon.awssdk.http SdkHttpClient]
           [software.amazon.awssdk.services.dynamodb DynamoDbClient DynamoDbClientBuilder]
           [software.amazon.awssdk.services.dynamodb.model ListTablesResponse]
           [software.amazon.awssdk.auth.credentials EnvironmentVariableCredentialsProvider]))

(def ^SdkHttpClient http-client
  (UrlConnectionHttpClient/create))

(def dynamodb-client
  (atom nil))

(defn initialize!
  []
  (reset! dynamodb-client
    (let [^EnvironmentVariableCredentialsProvider cred-provider (EnvironmentVariableCredentialsProvider/create)
          ^DynamoDbClientBuilder builder (DynamoDbClient/builder)
          ^DynamoDbClientBuilder with-http (.httpClient builder http-client)
          ^DynamoDbClientBuilder with-cred (.credentialsProvider with-http cred-provider)]
      (.build with-cred))))

(defn dynamodb-handler
  [input context]
  (let [^DynamoDbClient client @dynamodb-client
        ^ListTablesResponse response (.listTables client)]
    (.tableNames response)))

(defn echo-handler
  [input context]
  input)
