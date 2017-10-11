(ns schmorgurken.pre-process
  (:require (clojure [string :as s])))

(defn- source-to-model
  [source file-name]
  (map #(hash-map :file file-name :src-text %1 :line-number %2) (s/split-lines source) (iterate inc 1)))

(defn- strip-comments
  [lines]
  (filter #(not= \# (first (:text %))) lines))

(defn- strip-blank-lines
  [lines]
  (filter #(or (:pystring %) (not (s/blank? (:text %)))) lines))

(defn- trim-lines
  [lines]
  (for [line lines] (if (:pystring line) line (update line :text s/trim))))

(defn- find-pystring-close
  [in-lines opening-spaces result]
  (let [pattern (re-pattern (str "^" opening-spaces "\"\"\"[ |\t]*$" "|^" opening-spaces "(.*)$"))]
    (loop [lines in-lines, t []]
      (if-let [line (first lines)]
        (if-let [[_ line-contents] (re-find pattern (:src-text line))]
          (if line-contents
            (recur (rest lines) (conj t line-contents))
            [(rest lines) (conj result (assoc (first in-lines) :pystring (s/join "\n" t)))])
          (throw (ex-info "Indentation does not match opening triple-quoted line" line)))
        (throw (ex-info "No closing triple quote before end of file" (first in-lines)))))))

(defn- extract-pystrings
  [lines]
  (loop [lines lines, result []]
    (if-let [line (first lines)]
      (if-let [opening-spaces (second (re-find #"^([ |\t]*)\"\"\"[ |\t]*$" (:src-text line)))]
        (let [[lines result] (find-pystring-close (rest lines) opening-spaces result)]
          (recur lines result))
        (recur (rest lines) (conj result (assoc line :text (:src-text line)))))
      result)))

(defn source-to-lines
  "From input:
   1. remove blank lines, leading/trailing spaces and comments;
   2. process pystrings; and
   3. return a sequence of lines as maps of the line data containing
      the original :src-text, :line-number and whether it has a :pystring
      in the source or the :text of a line to be parsed
   The source is a String representation of the file and file-name is
   the name of the source file"
  [^CharSequence source ^String file-name]
  (-> source
      (source-to-model file-name)
      (extract-pystrings)
      (trim-lines)
      (strip-comments)
      (strip-blank-lines)))
