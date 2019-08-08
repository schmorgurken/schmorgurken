(ns schmorgurken.execute-test
  (:require [clojure.test :refer :all]
            [clojure.string :as s]
            [clojure.java.io :as io])
  (:import (clojure.lang ExceptionInfo))
  (:use (schmorgurken core))
  (:gen-class))

(deftest find-matching-step-defs
  (let [test-step-defs
        [(Given #"^that my position in (\w{6}) is (\d+) at ([\d.]+)$"
                (fn [_ cross qty price] (println cross " " qty " " price)))

         (And #"^the market for (\w{6}) is at \[([\d.]+);([\d.]+)\]$"
              (fn [_ cross buy-price sell-price] (println cross buy-price sell-price)))

         (When #"^I submit an order to (\w) (\d+) (\w{6}) at (\w)$"
               (fn [_ dirn qty cross ord-type] (println dirn qty cross ord-type)))

         (Then #"^a trade should be made at ([\d.]+)$"
               (fn [_ price dummy] (println price)))

         (And #"^my position should show (\w) (\d+) (\w{6}) at ([\d.]+)$"
              (fn [_ ls qty cross price] (println ls qty cross price)))]]

    (testing "Find matching step definition"
      (let [[fn args] (first (#'schmorgurken.execute/find-matching-step-defs
                               "the market for EURUSD is at [1.34662;1.34714]" test-step-defs))]
        (is (function? fn))
        (is (= '("EURUSD" "1.34662" "1.34714") args))))

    (testing "No matching step definition"
      (is (= '() (#'schmorgurken.execute/find-matching-step-defs "fred" test-step-defs))))

    (testing "Multiple matching step definitions"
      (is (= 2 (count (#'schmorgurken.execute/find-matching-step-defs
                        "the market for EURUSD is at [1.34662;1.34714]"
                        (conj test-step-defs
                              (Given #"^the market for (\w{6}) is at \[([\d.]+);([\d.]+)\]$"
                                     (fn [cross buy-price sell-price] (println cross buy-price sell-price)))))))))))

(deftest file-search
  (testing "find files"
    (let [root-path (.getPath (io/resource "findfilestest"))]
      (is (= #{(str root-path "/a/c/4.txt")
               (str root-path "/a/1.txt")
               (str root-path "/b/2.txt")
               (str root-path "/b/3.txt")}
             (set (map #(.getPath %1) (#'schmorgurken.execute/find-all-files-recursively "findfilestest"))))))))

(deftest execute-no-files
  (testing "no files"
    (is (thrown-with-msg? ExceptionInfo #"^No feature files found in the path specified$"
                          (#'schmorgurken.execute/-run-feature! "empty"
                            [(Given #"anything" #(%1))])))))

(deftest execute-failures
  (testing "wrong arity"
    (is (thrown-with-msg? ExceptionInfo #"^The step handler function has the wrong arity \(number of args\) when matching:$"
                          (#'schmorgurken.execute/-run-feature! "badfeatures/arity.feature"
                            [(Given #"^a single parameter (.*)$"
                                    (fn [_] (println "hello")))])))))

(run-tests)
