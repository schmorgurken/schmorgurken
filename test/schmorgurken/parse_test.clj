(ns schmorgurken.parse-test
  (:require [clojure.test :refer :all]
            [schmorgurken.parse :refer :all]
            [schmorgurken.pre-process :as pp])
  (:import (clojure.lang ExceptionInfo)))

(deftest test-is-match-given-when-then
  (testing "match given/when/then/and"
    (is (= nil (#'schmorgurken.parse/match-given-when-then nil)))
    (is (= "something new " (#'schmorgurken.parse/match-given-when-then {:text "Given something new "})))
    (is (= "something happens " (#'schmorgurken.parse/match-given-when-then {:text "When something happens "})))
    (is (= "something results " (#'schmorgurken.parse/match-given-when-then {:text "Then something results "})))
    (is (= "something else " (#'schmorgurken.parse/match-given-when-then {:text "And something else "})))
    (is (= nil (#'schmorgurken.parse/match-given-when-then {:text "Any random text"})))))

(deftest test-is-step-terminating-line
  (testing "lines that have keywords that would terminate step processing"
    (is (#'schmorgurken.parse/is-step-terminating-line? nil))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Scenario:"}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Scenario: This is a scenario "}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Scenario Outline:"}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Scenario Outline: This is a scenario outline"}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Background:"}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Background: Some background"}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Examples:"}))
    (is (#'schmorgurken.parse/is-step-terminating-line? {:text "Examples: Some examples"}))
    (is (not (#'schmorgurken.parse/is-step-terminating-line? {:text "Hello - some random line"})))
    (is (not (#'schmorgurken.parse/is-step-terminating-line? {:text "Given something"})))
    (is (not (#'schmorgurken.parse/is-step-terminating-line? {:text "When something"})))
    (is (not (#'schmorgurken.parse/is-step-terminating-line? {:text "Then something"})))
    (is (not (#'schmorgurken.parse/is-step-terminating-line? {:text "And something"})))))

(deftest test-match-initial-keyword
  (testing "match initial keyword"
    (is (= (#'schmorgurken.parse/match-initial-keyword nil) ()))
    (is (= (#'schmorgurken.parse/match-initial-keyword "") ()))
    (is (= (#'schmorgurken.parse/match-initial-keyword "fred") ()))
    (is (= (#'schmorgurken.parse/match-initial-keyword "Scenario: test") '("Scenario" "test")))
    (is (= (#'schmorgurken.parse/match-initial-keyword "Scenario Outline: test") '("Scenario Outline" "test")))
    (is (= (#'schmorgurken.parse/match-initial-keyword "Background: test") '("Background" "test")))
    (is (= (#'schmorgurken.parse/match-initial-keyword "Examples: test") ()))))

(deftest test-process-steps
  (testing "match steps with table, end of file terminating"
    (let [lines (pp/source-to-lines (str "Given hello\n"
                                         "And something more\n"
                                         "When something happens\n"
                                         "| col1 | col2 |\n"
                                         "| val1 | val2 |\n"
                                         "Then we're in a good state\n"
                                         "Now we're doing something else") "test")
          expected [[{:text        "hello"
                      :line-number 1
                      :src-text    "Given hello"
                      :file        "test"}
                     {:text        "something more"
                      :line-number 2
                      :src-text    "And something more"
                      :file        "test"}
                     {:text        "something happens"
                      :table       [{"col1" "val1", "col2" "val2"}]
                      :line-number 3
                      :src-text    "When something happens"
                      :file        "test"}
                     {:text        "we're in a good state"
                      :line-number 6
                      :src-text    "Then we're in a good state"
                      :file        "test"}] []]]

      (is (= expected (#'schmorgurken.parse/process-steps lines)))))

  (testing "match steps with table, next scenario terminating"
    (let [lines (pp/source-to-lines (str "Given hello\n"
                                         "And something more\n"
                                         "When something happens\n"
                                         "| col1 | col2 |\n"
                                         "| val1 | val2 |\n"
                                         "Then we're in a good state\n"
                                         "Now we're doing something else\n\n"
                                         "Scenario: This is the next scanario\n"
                                         "Given: something happens\n") "test")
          expected [[{:text        "hello"
                      :src-text    "Given hello"
                      :line-number 1
                      :file        "test"}
                     {:text        "something more"
                      :src-text    "And something more"
                      :line-number 2
                      :file        "test"}
                     {:text        "something happens"
                      :src-text    "When something happens"
                      :table       [{"col1" "val1", "col2" "val2"}]
                      :line-number 3
                      :file        "test"}
                     {:text        "we're in a good state"
                      :src-text    "Then we're in a good state"
                      :line-number 6
                      :file        "test"}]
                    [{:text        "Scenario: This is the next scanario"
                      :src-text    "Scenario: This is the next scanario"
                      :line-number 9
                      :file        "test"}
                     {:text        "Given: something happens"
                      :src-text    "Given: something happens"
                      :line-number 10
                      :file        "test"}]]]

      (is (= expected (#'schmorgurken.parse/process-steps lines))))))


(deftest test-parse
  (testing "Empty source file"
    (is (thrown-with-msg? ExceptionInfo #"^Source does not contain a <Feature: description>; the first non-blank line is$"
                          (parse (pp/source-to-lines "\n" "test")))))

  (testing "Just a feature"
    (is (= (parse (pp/source-to-lines "Feature: this is my feature\n" "test"))
           [])))

  (testing "Feature with single scenario"
    (is (= [{:scenario "Jeff returns a faulty microwave",
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given Jeff has bought a microwave for $100"
                         :text        "Jeff has bought a microwave for $100"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "And he has a receipt"
                         :text        "he has a receipt"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "When he returns the microwave"
                         :text        "he returns the microwave"}
                        {:file        "test"
                         :line-number 6
                         :src-text    "Then Jeff should be refunded $100"
                         :text        "Jeff should be refunded $100"}]}]

           (parse (pp/source-to-lines (str "Feature: Refund item\n"
                                           "Scenario: Jeff returns a faulty microwave\n"
                                           "Given Jeff has bought a microwave for $100\n"
                                           "And he has a receipt\n"
                                           "When he returns the microwave\n"
                                           "Then Jeff should be refunded $100\n") "test")))))

  (testing "Feature with single scenario and extra text"
    (is (= [{:scenario "Jeff returns a faulty microwave",
             :steps    [{:text        "Jeff has bought a microwave for $100"
                         :src-text    "Given Jeff has bought a microwave for $100"
                         :file        "test"
                         :line-number 10,}
                        {:text        "he has a receipt"
                         :src-text    "And he has a receipt"
                         :file        "test"
                         :line-number 11}
                        {:text        "he returns the microwave"
                         :src-text    "When he returns the microwave"
                         :file        "test"
                         :line-number 12}
                        {:text        "Jeff should be refunded $100"
                         :src-text    "Then Jeff should be refunded $100"
                         :file        "test"
                         :line-number 13}]}]

           (parse (pp/source-to-lines (str "Feature: Refund item\n"
                                           "Sales assistants should be able to refund customers' purchases.\n"
                                           "This is required by the law, and is also essential in order to\n"
                                           "keep customers happy.\n\n"
                                           "Rules:\n"
                                           " - Customer must present proof of purchase\n"
                                           " - Purchase must be less than 30 days ago\n"
                                           "Scenario: Jeff returns a faulty microwave\n"
                                           "Given Jeff has bought a microwave for $100\n"
                                           "And he has a receipt\n"
                                           "When he returns the microwave\n"
                                           "Then Jeff should be refunded $100\n") "test")))))

  (testing "Feature with multiple scenario"
    (is (= [{:scenario "feeding a small suckler cow",
             :steps    [{:text        "the cow weighs 450 kg"
                         :src-text    "Given the cow weighs 450 kg"
                         :file        "test"
                         :line-number 3}
                        {:text        "we calculate the feeding requirements"
                         :src-text    "When we calculate the feeding requirements"
                         :file        "test"
                         :line-number 4}
                        {:text        "the energy should be 26500 MJ"
                         :src-text    "Then the energy should be 26500 MJ"
                         :file        "test"
                         :line-number 5}
                        {:text        "the protein should be 215 kg"
                         :src-text    "And the protein should be 215 kg"
                         :file        "test"
                         :line-number 6}]}
            {:scenario "feeding a medium suckler cow"
             :steps    [{:text        "the cow weighs 500 kg"
                         :src-text    "Given the cow weighs 500 kg"
                         :file        "test"
                         :line-number 9}
                        {:text        "we calculate the feeding requirements"
                         :src-text    "When we calculate the feeding requirements"
                         :file        "test"
                         :line-number 10}
                        {:text        "the energy should be 29500 MJ"
                         :src-text    "Then the energy should be 29500 MJ"
                         :file        "test"
                         :line-number 11}
                        {:text        "the protein should be 245 kg"
                         :src-text    "And the protein should be 245 kg"
                         :file        "test"
                         :line-number 12}]}]

           (parse (pp/source-to-lines (str "Feature: Feed planning\n"
                                           "Scenario: feeding a small suckler cow\n"
                                           "Given the cow weighs 450 kg\n"
                                           "When we calculate the feeding requirements\n"
                                           "Then the energy should be 26500 MJ\n"
                                           "And the protein should be 215 kg\n\n"
                                           "Scenario: feeding a medium suckler cow\n"
                                           "Given the cow weighs 500 kg\n"
                                           "When we calculate the feeding requirements\n"
                                           "Then the energy should be 29500 MJ\n"
                                           "And the protein should be 245 kg\n"), "test")))))

  (testing "Feature with scenario outline"
    (is (= [{:scenario "feeding a suckler cow",
             :steps    [{:text        "the cow weighs 450 kg"
                         :src-text    "Given the cow weighs <weight> kg"
                         :file        "test"
                         :line-number 3}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When we calculate the feeding requirements"
                         :text        "we calculate the feeding requirements"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "Then the energy should be <energy> MJ"
                         :text        "the energy should be 26500 MJ"}
                        {:file        "test"
                         :line-number 6
                         :src-text    "And the protein should be <protein> kg"
                         :text        "the protein should be 215 kg"}]}
            {:scenario "feeding a suckler cow"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given the cow weighs <weight> kg"
                         :text        "the cow weighs 500 kg"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When we calculate the feeding requirements"
                         :text        "we calculate the feeding requirements"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "Then the energy should be <energy> MJ"
                         :text        "the energy should be 29500 MJ"}
                        {:file        "test"
                         :line-number 6
                         :src-text    "And the protein should be <protein> kg"
                         :text        "the protein should be 245 kg"}]}
            {:scenario "feeding a suckler cow"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given the cow weighs <weight> kg"
                         :text        "the cow weighs 575 kg"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When we calculate the feeding requirements"
                         :text        "we calculate the feeding requirements"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "Then the energy should be <energy> MJ"
                         :text        "the energy should be 31500 MJ"}
                        {:file        "test"
                         :line-number 6
                         :src-text    "And the protein should be <protein> kg"
                         :text        "the protein should be 255 kg"}]}
            {:scenario "feeding a suckler cow"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given the cow weighs <weight> kg"
                         :text        "the cow weighs 600 kg"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When we calculate the feeding requirements"
                         :text        "we calculate the feeding requirements"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "Then the energy should be <energy> MJ"
                         :text        "the energy should be 37000 MJ"}
                        {:file        "test"
                         :line-number 6
                         :src-text    "And the protein should be <protein> kg"
                         :text        "the protein should be 305 kg"}]}]

           (parse (pp/source-to-lines (str "Feature: Feed planning\n"
                                           "Scenario Outline: feeding a suckler cow\n"
                                           "Given the cow weighs <weight> kg\n"
                                           "When we calculate the feeding requirements\n"
                                           "Then the energy should be <energy> MJ\n"
                                           "And the protein should be <protein> kg\n\n"
                                           "Examples:\n"
                                           "| weight | energy | protein |\n"
                                           "| 450 | 26500 | 215 |\n"
                                           "| 500 | 29500 | 245 |\n"
                                           "| 575 | 31500 | 255 |\n"
                                           "| 600 | 37000 | 305 |\n") "test")))))

  (testing "Feature with scenario outline with empty examples"
    (is (= []
           (parse (pp/source-to-lines (str "Feature: Feed planning\n"
                                           "Scenario Outline: feeding a suckler cow\n"
                                           "Given the cow weighs <weight> kg\n"
                                           "When we calculate the feeding requirements\n"
                                           "Then the energy should be <energy> MJ\n"
                                           "And the protein should be <protein> kg\n\n"
                                           "Examples:\n"
                                           "| weight | energy | protein |\n"
                                           "Scenario:\n") "test")))))

  (testing "Feature with scenario outline with bad variable name"
    (is (thrown-with-msg? ExceptionInfo #"^Unknown variable in scenario outline$"
                          (doall (parse (pp/source-to-lines (str "Feature: Feed planning\n"
                                                                 "Scenario Outline: feeding a suckler cow\n"
                                                                 "Given the cow weighs <weight> kg\n"
                                                                 "When we calculate the feeding requirements\n"
                                                                 "Then the energy should be <energy> MJ\n"
                                                                 "And the protein should be <HELLO> kg\n\n"
                                                                 "Examples:\n"
                                                                 "| weight | energy | protein |\n"
                                                                 "| 450 | 26500 | 215 |\n"
                                                                 "| 500 | 29500 | 245 |\n"
                                                                 "| 575 | 31500 | 255 |\n"
                                                                 "| 600 | 37000 | 305 |\n") "test"))))))

  (testing "Feature with multiple scenario and a background"
    (is (= [{:scenario "feeding a small suckler cow"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given a given background"
                         :text        "a given background"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When a when background"
                         :text        "a when background"}
                        {:file        "test"
                         :line-number 6
                         :src-text    "Given the cow weighs 450 kg"
                         :text        "the cow weighs 450 kg"}
                        {:file        "test"
                         :line-number 7
                         :src-text    "When we calculate the feeding requirements"
                         :text        "we calculate the feeding requirements"}
                        {:file        "test"
                         :line-number 8
                         :src-text    "Then the energy should be 26500 MJ"
                         :text        "the energy should be 26500 MJ"}
                        {:file        "test"
                         :line-number 9
                         :src-text    "And the protein should be 215 kg"
                         :text        "the protein should be 215 kg"}]}
            {:scenario "feeding a medium suckler cow"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given a given background"
                         :text        "a given background"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When a when background"
                         :text        "a when background"}
                        {:file        "test"
                         :line-number 12
                         :src-text    "Given the cow weighs 500 kg"
                         :text        "the cow weighs 500 kg"}
                        {:file        "test"
                         :line-number 13
                         :src-text    "When we calculate the feeding requirements"
                         :text        "we calculate the feeding requirements"}
                        {:file        "test"
                         :line-number 14
                         :src-text    "Then the energy should be 29500 MJ"
                         :text        "the energy should be 29500 MJ"}
                        {:file        "test"
                         :line-number 15
                         :src-text    "And the protein should be 245 kg"
                         :text        "the protein should be 245 kg"}]}]

           (parse (pp/source-to-lines (str "Feature: Feed planning\n"
                                           "Background:\n"
                                           "Given a given background\n"
                                           "When a when background\n"
                                           "Scenario: feeding a small suckler cow\n"
                                           "Given the cow weighs 450 kg\n"
                                           "When we calculate the feeding requirements\n"
                                           "Then the energy should be 26500 MJ\n"
                                           "And the protein should be 215 kg\n\n"
                                           "Scenario: feeding a medium suckler cow\n"
                                           "Given the cow weighs 500 kg\n"
                                           "When we calculate the feeding requirements\n"
                                           "Then the energy should be 29500 MJ\n"
                                           "And the protein should be 245 kg\n") "test")))))

  (testing "Feature with background and scenario outline"
    (is (= [{:scenario "Eating"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given there are 12 cucumbers"
                         :text        "there are 12 cucumbers"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When I eat 5 cucumbers"
                         :text        "I eat 5 cucumbers"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "Then I should have 7 cucumbers"
                         :text        "I should have 7 cucumbers"}
                        {:file        "test"
                         :line-number 7
                         :src-text    "Given there are <start> cucumbers"
                         :text        "there are 17 cucumbers"}
                        {:file        "test"
                         :line-number 8
                         :src-text    "When I eat <eat> cucumbers"
                         :text        "I eat 2 cucumbers"}
                        {:file        "test"
                         :line-number 9
                         :src-text    "Then I should have <left> cucumbers"
                         :text        "I should have 15 cucumbers"}]}
            {:scenario "Eating"
             :steps    [{:file        "test"
                         :line-number 3
                         :src-text    "Given there are 12 cucumbers"
                         :text        "there are 12 cucumbers"}
                        {:file        "test"
                         :line-number 4
                         :src-text    "When I eat 5 cucumbers"
                         :text        "I eat 5 cucumbers"}
                        {:file        "test"
                         :line-number 5
                         :src-text    "Then I should have 7 cucumbers"
                         :text        "I should have 7 cucumbers"}
                        {:file        "test"
                         :line-number 7
                         :src-text    "Given there are <start> cucumbers"
                         :text        "there are 20 cucumbers"}
                        {:file        "test"
                         :line-number 8
                         :src-text    "When I eat <eat> cucumbers"
                         :text        "I eat 6 cucumbers"}
                        {:file        "test"
                         :line-number 9
                         :src-text    "Then I should have <left> cucumbers"
                         :text        "I should have 14 cucumbers"}]}]

           (parse (pp/source-to-lines (str "Feature: a test\n"
                                           "Background:\n"
                                           "Given there are 12 cucumbers\n"
                                           "When I eat 5 cucumbers\n"
                                           "Then I should have 7 cucumbers\n"
                                           "Scenario Outline: Eating\n"
                                           "Given there are <start> cucumbers\n"
                                           "When I eat <eat> cucumbers\n"
                                           "Then I should have <left> cucumbers\n"
                                           "Examples:\n"
                                           "| start | eat | left |\n"
                                           "| 17 | 2 | 15 |\n"
                                           "| 20 | 6 | 14 |\n") "test")))))

  (testing "Scenario with a given with a pystring"
    (is (= [{:scenario "Blog post"
             :steps    [{:pystring    (str "Some Title, Eh?\n"
                                           "===============\n"
                                           "Here is the first paragraph of my blog post. Lorem ipsum dolor sit amet,\n"
                                           "consectetur adipiscing elit."),
                         :file        "test"
                         :line-number 3
                         :text        "a blog post named \"Random\" with Markdown body"
                         :src-text    "Given a blog post named \"Random\" with Markdown body"}]}]

           (parse (pp/source-to-lines (str "Feature: blogging\n"
                                           "Scenario: Blog post\n"
                                           "Given a blog post named \"Random\" with Markdown body\n"
                                           "  \"\"\"\n"
                                           "  Some Title, Eh?\n"
                                           "  ===============\n"
                                           "  Here is the first paragraph of my blog post. Lorem ipsum dolor sit amet,\n"
                                           "  consectetur adipiscing elit.\n"
                                           "  \"\"\"\n") "test")))))

  (testing "Given without an argument"
    (is (= []
           (parse (pp/source-to-lines (str "Feature: a test\n"
                                           "Scenario: Something to say\n"
                                           "Given \n") "test")))))

  (testing "Scenario outline with missing examples"
    (is (thrown-with-msg? ExceptionInfo #"^Missing examples for scenario outline$"
                          (parse (pp/source-to-lines (str "Feature: a test\n"
                                                          "Given there are 12 cucumbers\n"
                                                          "When I eat 5 cucumbers\n"
                                                          "Then I should have 7 cucumbers\n"
                                                          "Scenario Outline: Eating\n"
                                                          "Given there are <start> cucumbers\n"
                                                          "When I eat <eat> cucumbers\n"
                                                          "Then I should have <left> cucumbers\n"
                                                          "Hello") "test"))))))

(run-tests)
