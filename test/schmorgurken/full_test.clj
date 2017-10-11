(ns schmorgurken.full-test
  (:require [clojure.test :refer :all])
  (:use (schmorgurken core)))

; Note - this test deliberately loads two feature files so as to test
; the state management - state from the first test should not pollute
; the second test
(Feature "features"
         (Given #"^the cow weighs (\d+) kg$" (fn [_ v] {:wt (bigint v)}))

         (When #"^we calculate the feeding requirements$"
               (fn [cow]
                 (case (:wt cow)
                   450 (assoc cow :energy 26500 :protein 215)
                   500 (assoc cow :energy 29500 :protein 245)
                   575 (assoc cow :energy 31500 :protein 255)
                   600 (assoc cow :energy 37000 :protein 305)
                   (is false))))

         (Then #"^the energy should be (\d+) MJ$" (fn [cow energy]
                                                    (is (= (:energy cow) (bigint energy)))
                                                    cow))

         (And #"^the protein should be (\d+) kg$" (fn [cow protein]
                                                    (is (= (:protein cow) (bigint protein)))
                                                    cow))

         ; calculator

         (Given #"^two numbers (.*) and (.*)$"
                (fn [state a b]
                  (is (nil? state))
                  {:a a :b b}))

         (When #"^we calculate$"
               (fn [state] (+ (bigint (state :a)) (bigint (state :b)))))

         (Then #"^the result should be (.*)$",
               (fn [state result] (is (= (bigint result) state))))

         (When #"^we multiply$"
               (fn [_ examples]
                 (loop [examples examples]
                   (when-let [example (first examples)]
                     (is (= (bigint (example "result"))
                            (* (bigint (example "a")) (bigint (example "b")))))
                     (recur (next examples))))))

         ; background test - login

         (Given #"^there are (.*) users logged in$"
                (fn [_ count-users] (bigint count-users)))

         (When #"^a new user logs in$"
               (fn [existing-count] (inc existing-count)))

         (When #"^the message should be$"
               (fn [_ message]
                 (is (= "Welcome to the platform\nThere are now 16 users logged into the system" message))))

         (When #"^a user logs out$"
               (fn [existing-count] (dec existing-count)))

         (Then #"^the number of users should now be (.*)$"
               (fn [count expected-count] (is (= (bigint expected-count) count)))))

(require 'pjstadig.humane-test-output)
(pjstadig.humane-test-output/activate!)
(run-tests)
