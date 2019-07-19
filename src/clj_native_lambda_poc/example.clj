(ns clj-native-lambda-poc.example)

(defn echo-handler
  [input context]
  (assoc input :success true :context context))
