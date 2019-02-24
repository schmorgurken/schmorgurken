(defproject schmorgurken "0.1.2"
  :description "Native Clojure Gherkin BDD tool"
  :url "https://github.com/schmorgurken/schmorgurken"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.10.0"]]
  :jar-exclusions [#"test/.*" #"features/.*" #"empty/.*"]
  :signing {:gpg-key "schmorgurken@anamatica.com"}
  :target-path "target/%s")
