(defproject schmorgurken "0.1.0"
  :description "Native Clojure Gherkin BDD tool"
  :url "https://github.com/schmorgurken/schmorgurken"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [pjstadig/humane-test-output "0.8.2"]]
  :jar-exclusions [#"test/.*" #"features/.*" #"empty/.*"]
  :signing {:gpg-key "schmorgurken@anamatica.com"}
  :target-path "target/%s")
