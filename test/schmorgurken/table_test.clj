(ns schmorgurken.table_test
  (:require [clojure.test :refer :all]
            [schmorgurken.table :refer :all]
            [schmorgurken.pre-process :refer :all])
  (:import (clojure.lang ExceptionInfo)))

(deftest test-data-table
  (testing "Data table: nil source"
    (is (= [nil nil] (read-data-table nil))))

  (testing "Data table: empty source"
    (is (= [nil '()] (read-data-table '()))))

  (testing "Data table: no data table in the source"
    (let [lines (source-to-lines "hello\na test line" "test")]
      (is (= [nil lines] (read-data-table lines)))))

  (testing "Data table: header but no rows"
    (is (= [() ()]
           (read-data-table
             (source-to-lines "| name | address        | telephone    |\n" "test")))))

  (testing "Data table: simple three column table"
    (is (= [(list {"name"      "Nick"
                   "address"   "Huntingdon"
                   "telephone" "01832 123456"}
                  {"name"      "Bill"
                   "address"   "Wellingborough"
                   "telephone" "01933 123456"}) ()]

           (read-data-table
             (source-to-lines (str "| name | address        | telephone    |\n"
                                   "| Nick | Huntingdon     | 01832 123456 |\n"
                                   "| Bill | Wellingborough | 01933 123456 |") "test")))))

  (testing "Data table: remainder lines correctly returned"
    (let [lines (source-to-lines (str "| name | address        | telephone    |\n"
                                      "| Nick | Huntingdon     | 01832 123456 |\n"
                                      "\n"
                                      "And now for some random text") "test")]

      (is (= [(list {"name"      "Nick"
                     "address"   "Huntingdon"
                     "telephone" "01832 123456"}) (drop 2 lines)]
             (read-data-table lines)))))

  (testing "Data table: badly formed table - mismatch between columns and rows"
    (is (thrown-with-msg? ExceptionInfo #"^Badly formed row in table - doesn't match header column count$"
                          (doall (first (read-data-table
                                          (source-to-lines (str "| name | address        | telephone    |\n"
                                                                "| Nick | Huntingdon     | \n"
                                                                "| Bill | Wellingborough | 01933 123456 |") "test")))))))

  (testing "Data table: no column names defined in table"
    (is (thrown-with-msg? ExceptionInfo #"^No column names defined in table$"
                          (read-data-table
                            (source-to-lines "|||" "test")))))

  (testing "Data table: table with escaped delimiter"
    (is (= [(list {"name"      "Nick"
                   "address"   "Huntingdon |"
                   "telephone" "01832 123456"}
                  {"name"      "Bill"
                   "address"   "Wellingborough\\"
                   "telephone" "01933 123456"}
                  {"name"      "\t\r\n\f\b|"
                   "address"   "more data ||qw"
                   "telephone" "\\"}
                  {"name"      " Fred"
                   "address"   "London   "
                   "telephone" "020 7123 4567"}) ()]

           (read-data-table
             (source-to-lines (str "| name | address             | telephone    |\n"
                                   "| Nick | Huntingdon \\|      | 01832 123456 |\n"
                                   "| Bill | Wellin\\gborough\\\\| 01933 123456 |\n"
                                   "| \\t\\r\\n\\f\\b\\| | more data \\|\\|\\q\\w| \\\\|\n"
                                   "|\\ Fred | London \\ \\ |     020 7123 4567   |\n"
                                   ) "test"))))))

(run-tests)
