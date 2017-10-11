(ns schmorgurken.pre-process-test
  (:require [clojure.test :refer :all]
            [schmorgurken.pre-process :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest test-basic-processing
  (testing "line numbered source")
  (is (= (list {:line-number 1
                :src-text    "hello"
                :text        "hello"
                :file        "test"}
               {:line-number 2
                :src-text    "goodbye"
                :text        "goodbye"
                :file        "test"}
               {:line-number 3
                :src-text    "apt"
                :text        "apt"
                :file        "test"})

         (source-to-lines "hello\ngoodbye\napt\n" "test")))

  (testing "remove comments from the source file"
    (is (= (list {:line-number 1
                  :src-text    "a"
                  :text        "a"
                  :file        "test"}
                 {:line-number 5
                  :src-text    "# this is NOT a comment"
                  :pystring    "# this is NOT a comment"
                  :file        "test"}
                 {:line-number 7
                  :src-text    "b"
                  :text        "b"
                  :file        "test"})

           (source-to-lines (str "a\n"
                                 "# this is a comment\n"
                                 " # this is a comment too\n"
                                 "\"\"\"\n"
                                 "# this is NOT a comment\n"
                                 "\"\"\"\n"
                                 "b\n") "test"))))

  (testing "trim all lines in the source file/strip blank lines"
    (is (= (list {:line-number 1
                  :src-text    " hello this is a test "
                  :text        "hello this is a test"
                  :file        "test"},
                 {:line-number 4
                  :src-text    "      "
                  :pystring    "     "
                  :file        "test"})

           (source-to-lines (str " hello this is a test \n"
                                 "     \n"
                                 " \"\"\"\n"
                                 "      \n"
                                 " \"\"\"\n") "test")))))

(deftest test-extract-pystrings
  (testing "extract and process all the python strings in the file"
    (is (= (list {:line-number 1
                  :src-text    "a"
                  :text        "a"
                  :file        "test"}
                 {:line-number 3
                  :src-text    "  this is a test"
                  :pystring    "this is a test\n this is a test"
                  :file        "test"}
                 {:line-number 6
                  :src-text    "\"\"\"hello\"\"\""
                  :text        "\"\"\"hello\"\"\""
                  :file        "test"}
                 {:line-number 7
                  :src-text    "b"
                  :text        "b"
                  :file        "test"})

           (source-to-lines (str "a\n"
                                 "  \"\"\"\n"
                                 "  this is a test\n"
                                 "   this is a test\n"
                                 "  \"\"\"\n"
                                 "\"\"\"hello\"\"\"\n"
                                 "b\n") "test"))))

  (testing "extract a pystring from the file, but has no closing quotes and hence is an error"
    (is (thrown-with-msg? ExceptionInfo #"^No closing triple quote before end of file$"
                          (source-to-lines (str "a\n"
                                                " \"\"\"\n"
                                                " this is a test\n"
                                                "  this is a test\n"
                                                " b\n") "test"))))

  (testing "extract a pystring from the file, but indentation isn't matched and hence causes an error"
    (is (thrown-with-msg? ExceptionInfo #"^Indentation does not match opening triple-quoted line$"
                          (source-to-lines (str "a\n"
                                                " \"\"\"\n"
                                                " this is a test\n"
                                                "this is a test\n"
                                                " \"\"\"\n"
                                                "b\n") "test")))))

(deftest test-pre-process-source-to-lines
  (testing "process source file ready for parsing"
    (is (= (list {:line-number 1
                  :src-text    "A line  "
                  :text        "A line"
                  :file        "test"}
                 {:line-number 3
                  :src-text    " Another line"
                  :text        "Another line"
                  :file        "test"}
                 {:line-number 5
                  :src-text    "  A pystring"
                  :pystring    "A pystring\n# with something that looks like a comment"
                  :file        "test"})

           (source-to-lines (str "A line  \n"
                                 "# a comment \n"
                                 " Another line\n"
                                 "  \"\"\"\n"
                                 "  A pystring\n"
                                 "  # with something that looks like a comment\n"
                                 "  \"\"\"") "test")))))

(require 'pjstadig.humane-test-output)
(pjstadig.humane-test-output/activate!)
(run-tests)

