(ns schmorgurken.parse
  (:require (clojure [string :as s])
            (schmorgurken [table :as tbl])))

(defn- match-initial-keyword
  [^String line-text]
  (rest (re-matches #"^(Scenario Outline|Scenario|Background):\s*(.*)$" (str line-text))))

(defn- with-pystring
  [lines]
  [{:pystring (:pystring (second lines))} (drop 2 lines)])

(defn- with-table
  [lines]
  (let [[tbl lines] (tbl/read-data-table (rest lines))]
    [(when tbl {:table tbl}) lines]))

(defn- on-given-when-then
  [steps step lines]
  (let [[r lines] (if (:pystring (second lines)) (with-pystring lines) (with-table lines))]
    [(conj steps (merge r step)) lines]))

(defn- match-given-when-then
  [{:keys [text]}]
  (when text (fnext (re-matches #"^(?:Given|When|Then|And)\s+(.*)$" text))))

(defn- is-step-terminating-line?
  [{:keys [text]}]
  (or (nil? text) (seq (re-matches #"^(Scenario Outline|Scenario|Background|Examples):.*$" text))))

(defn- process-steps
  [lines]
  (loop [[steps lines] [[] lines]]
    (let [line (first lines)]
      (if-let [match-text (match-given-when-then line)]
        (recur (on-given-when-then steps (assoc line :text match-text) lines))
        (if-not (is-step-terminating-line? line)
          (recur [steps (rest lines)])
          [steps lines])))))

(defn- is-examples-keyword?
  [line-text]
  (s/starts-with? line-text "Examples:"))

(defn- substitute-variables-in-text
  [v step]
  (assoc step :text (s/replace (:text step) #"<([^<]+)>"
                               #(if-let [r (v (second %1))]
                                  r
                                  (throw (ex-info "Unknown variable in scenario outline" step))))))

(defn- expand-examples-in-scenarios
  [scenario-desc steps examples]
  (for [table-entry examples]
    {:scenario scenario-desc
     :steps    (for [step steps] (substitute-variables-in-text table-entry step))}))

(defn- process-examples
  [scenario-desc steps lines]
  (let [[examples lines] (tbl/read-data-table lines)]
    (if-not examples
      (throw (ex-info "Missing table in examples section" (first lines)))
      [(expand-examples-in-scenarios scenario-desc steps examples) nil lines])))

(defn- process-steps-for-scenario-outline
  [in-lines]
  (loop [steps [], lines in-lines]
    (let [first-line (first lines)
          match-text (match-given-when-then first-line)]
      (cond
        (empty? first-line) (throw (ex-info "Missing examples for scenario outline" (first in-lines)))
        (not (nil? match-text)) (recur (conj steps (assoc first-line :text match-text)) (rest lines))
        (is-examples-keyword? (:text first-line)) [steps (rest lines)]
        :else (recur steps (rest lines))))))

(defn- on-scenario-outline
  [scenario-desc lines]
  (let [[steps lines] (process-steps-for-scenario-outline lines)]
    (process-examples scenario-desc steps lines)))

(defn- on-background
  [lines]
  (cons nil (process-steps lines)))

(defn- on-scenario
  [scenario lines]
  (let [[steps lines] (process-steps lines)] [[{:scenario scenario :steps steps}] nil lines]))

(defn- add-backgrounds-to-scenarios
  [all-scenarios all-backgrounds]
  (for [scenario all-scenarios] (update-in scenario [:steps] (partial concat all-backgrounds))))

(defn- parse-body
  [lines]
  (loop [all-scenarios [], all-backgrounds [], lines lines]
    (if-not (seq lines)
      (add-backgrounds-to-scenarios all-scenarios all-backgrounds)
      (let [[gherkin-keyword description] (match-initial-keyword (:text (first lines)))
            [scenarios backgrounds lines] (case gherkin-keyword
                                            "Scenario" (on-scenario description (rest lines))
                                            "Scenario Outline" (on-scenario-outline description (rest lines))
                                            "Background" (on-background (rest lines))
                                            [nil nil (rest lines)])]
        (recur (concat all-scenarios scenarios) (concat all-backgrounds backgrounds) lines)))))

(defn- parse-feature-statement
  [lines]
  (if (and (seq lines) (s/starts-with? (:text (first lines)) "Feature:"))
    (rest lines)
    (throw (ex-info "Source does not contain a <Feature: description>; the first non-blank line is" (merge {} (first lines))))))

(defn- is-non-empty-scenario?
  [scenario]
  (seq (:steps scenario)))

(defn parse
  "Parses the lines into a structure ready for test
  execution.  Lines must be a Seq of maps containing
  the :text and :line-number in the :file.

  Returns a structure
  [  {:scenario <scenario name>
        :steps ({:file <file name> :line-number <line number> :text <text to match>},
                {:file <file name> :line-number <line number> :text <text to match>} ... },
      :scenario <scenario name>
        :steps ({:file <file name> :line-number <line number> :text <text to match>},
                ... },
        ...]"
  [lines]
  (->> lines
      (parse-feature-statement)
      (parse-body)
      (filter is-non-empty-scenario?)))
