(defproject schmorgurken "0.1.0-SNAPSHOT"
  :description "Native Gherkin BDD tool for Clojure"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [pjstadig/humane-test-output "0.8.2"]]
  :main ^:skip-aot schmorgurken.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})

