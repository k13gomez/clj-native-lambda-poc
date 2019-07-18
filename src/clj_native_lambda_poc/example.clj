(ns clj-native-lambda-poc.example)

(defn echo-handler
  [{:keys [body]}]
  body)
