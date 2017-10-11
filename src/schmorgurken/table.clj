(ns schmorgurken.table
  (:require (clojure [string :as s])))

(defn- string->tokenized
  [in-s]
  (reduce (fn [output ch]
            (if (= (last output) :escape)
              (conj (subvec output 0 (dec (count output)))
                    (case ch
                      \n \newline
                      \t \tab
                      \b \backspace
                      \r \return
                      \f \formfeed
                      ch))
              (conj output (case ch
                             \\ :escape
                             \| :separator
                             \space :space
                             ch)))) [] in-s))

(defn- trim-string
  [tok-s]
  (reverse (drop-while (partial = :space) (reverse (drop-while (partial = :space) tok-s)))))

(defn- tokenized->string
  [s]
  (apply str (replace {:space \space} (trim-string s))))

(defn- extract-data-row
  [{:keys [text]}]
  (->> text
       (string->tokenized)
       (partition-by (partial not= :separator))
       (filter (partial not= (list :separator)))
       (map tokenized->string)))

(defn- is-table-line?
  [{:keys [text]}]
  (= (first text) \|))

(defn- extract-col-keys-from-first-line
  [{:keys [text]}]
  (rest (s/split text #"\s*\|\s*")))

(defn- extract-data-rows
  [column-keys lines]
  (let [[lines remaining] (split-with is-table-line? lines)]
    [(for [l lines :let [row-data (extract-data-row l)]]
       (if (= (count row-data) (count column-keys))
         (zipmap column-keys row-data)
         (throw (ex-info "Badly formed row in table - doesn't match header column count" l)))) remaining]))

(defn read-data-table
  "Reads the first line and if it is a data table then returns
  the data table that follows plus the remaining lines
  This supports escaping of characters and also allows escaping
  of the delimiter (|) and spaces (to allow blank spaces at the
  head and tail of the column data)"
  [lines]
  (if (is-table-line? (first lines))
    (if-let [column-keys (seq (extract-col-keys-from-first-line (first lines)))]
      (extract-data-rows column-keys (rest lines))
      (throw (ex-info "No column names defined in table" (first lines))))
    [nil lines]))                                           ; case of not matching a data table
