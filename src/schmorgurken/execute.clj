(ns schmorgurken.execute
  (:require (clojure [test :refer :all])
            (schmorgurken [parse :as pa]
                          [pre-process :as pp])
            (clojure.java [io :as io]))
  (:import (clojure.lang ExceptionInfo ArityException)
           (java.io File)))

(defn- find-matching-step-defs
  [match-text step-defs]
  (keep #(let [match (re-matches (:re %) match-text)]
           (cond
             (nil? match) nil
             (string? match) [(:func %) nil]                ; dumb implementation in regex - returns string if no capture groups!
             :else [(:func %) (rest match)]))               ; better - array of string results - string + captures
        step-defs))

(defn- exec-step-handler
  [state step func args]
  (try
    (cond
      (:table step) (apply func state (:table step) args)
      (:pystring step) (apply func state (:pystring step) args)
      :else (apply func state args))
    (catch ArityException _
      (throw (ex-info "The step handler function has the wrong arity (number of args) when matching:" step)))))

(defn- run-test-for-step
  [state step step-defs]
  (binding [*testing-contexts* (conj *testing-contexts* (:scenario step) (:text step))] ; integration into clojure.test
    (let [matches (find-matching-step-defs (:text step) step-defs)]
      (case (count matches)
        1 (let [[func args] (first matches)] (exec-step-handler state step func args))
        0 (throw (ex-info "Cannot find a step handler that matches the step" step))
        (throw (ex-info "More than one step handler matches the step" (merge step {:text matches})))))))

(defn- run-features
  [scenarios step-defs]
  (for [scenario scenarios :let [steps (:steps scenario)]]
            (reduce (fn [state step] (run-test-for-step state step step-defs)) nil steps)))

(defn- load-scenarios
  [files]
  (flatten (for [file files] (pa/parse (pp/source-to-lines (slurp file) file)))))

(defn- find-all-files-recursively
  [^String file-location]
  (some->> file-location
           (io/resource)
           (io/as-file)
           (file-seq)
           (filter #(not (.isDirectory ^File %)))
           (filter #(not (.isHidden ^File %)))
           (seq)))

(defn- -run-feature!
  [file-location step-defs]
  (if-let [files (find-all-files-recursively file-location)]
    (doall (run-features (load-scenarios files) step-defs))
    (throw (ex-info "No feature files found in the path specified" {:file file-location}))))

(defn- fmt-error
  [msg {:keys [file line-number text]}]
  (str msg (if text (str " \"" text "\" in ") " : ") file (when line-number (str " at line " line-number))))

(defn run-feature!
  "Set the location of the feature file(s) and define the step
  handlers to run from the features"
  [file-location step-defs]
  (try (-run-feature! file-location step-defs)
       (catch ExceptionInfo e
         (with-test-out
           (inc-report-counter :fail)
           (println "FAIL when processing test")
           (println (fmt-error (.getMessage e) (ex-data e)))))))
