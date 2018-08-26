(defproject schmorgurken "0.1.1"
  :description "Native Clojure Gherkin BDD tool"
  :url "https://github.com/schmorgurken/schmorgurken"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]]
  :profiles {:dev {:dependencies [[pjstadig/humane-test-output "0.8.3"]]}}
  :jar-exclusions [#"test/.*" #"features/.*" #"empty/.*"]
  :signing {:gpg-key "schmorgurken@anamatica.com"}
  :target-path "target/%s")
