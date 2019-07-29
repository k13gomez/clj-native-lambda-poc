(defproject clj-native-lambda-poc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure             "1.9.0"]
                 [cheshire                        "5.8.1"]
                 [org.martinklepsch/clj-http-lite "0.4.1"]
                 [org.clojure/tools.logging       "0.4.1"]
                 [ch.qos.logback/logback-classic  "1.2.3"]
                 [org.clojure/java.jdbc "0.7.9"]
                 [org.postgresql/postgresql "42.2.6"]
                 [software.amazon.awssdk/dynamodb "2.7.8"]
                 [software.amazon.awssdk/s3 "2.7.8"]
                 [software.amazon.awssdk/url-connection-client "2.7.8"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]
            [lein-with-env-vars "0.2.0"]]
  :main clj-native-lambda-poc.core
  :target-path "target/%s"
  :global-vars {*warn-on-reflection* true}
  :env-vars {:_HANDLER "clj-native-lambda-poc.example/echo-handler"
             :DB_URI "jdbc:postgresql://localhost:5432/postgres"
             :DB_USER "postgres"
             :DB_PASS "postgres"}
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :native-image {:name  "bootstrap"
                                      :opts ["-H:ReflectionConfigurationFiles=./reflection-config.json"
                                             "--initialize-at-run-time=org.postgresql.sspi.SSPIClient"
                                             "--allow-incomplete-classpath"
                                             "--initialize-at-build-time"
                                             "--enable-http"
                                             "--enable-https"
                                             "--enable-all-security-services"
                                             ;"--report-unsupported-elements-at-runtime"
                                             "--no-server"
                                             "--no-fallback"
                                             "--verbose"]}}})
