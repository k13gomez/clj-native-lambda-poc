(defproject clj-native-lambda-poc "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure             "1.9.0"]
                 [org.clojure/tools.logging       "0.4.1"]
                 [cheshire                        "5.8.1"]
                 [org.martinklepsch/clj-http-lite "0.4.1"]]
  :plugins [[io.taylorwood/lein-native-image "0.3.0"]]
  :main clj-native-lambda-poc.core
  :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :native-image {:name  "bootstrap"
                                      :opts ["--initialize-at-build-time"
                                             "--enable-http"
                                             "--enable-https"
                                             "--enable-all-security-services"
                                             ;"--report-unsupported-elements-at-runtime"
                                             "--no-server"
                                             "--no-fallback"
                                             "--verbose"]}}})
