(ns schmorgurken.core
  (:require (clojure [test :refer :all])
            (schmorgurken [execute :as ex])))

(defmacro Given [re func] `{:re ~re :func ~func})
(defmacro When [re func] `{:re ~re :func ~func})
(defmacro Then [re func] `{:re ~re :func ~func})
(defmacro And [re func] `{:re ~re :func ~func})

(defmacro Feature
  "Wires the test specification into the standard clojure
  test runner.  Hence you can use all of the standard
  tools, reporting, etc."
  [file-location & step-defs]
  (when *load-tests*
    (let [name `feature-test#]
      `(def ~(vary-meta name assoc :test `(fn [] (ex/run-feature! ~file-location [~@step-defs])))
         (fn [] (test-var (var ~name)))))))
