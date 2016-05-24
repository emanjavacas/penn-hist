(ns penn-hist.middleware
  (:require [clojure.string :as str]
            [penn-hist.build-lexicon :refer [read-lexicon has-dash split-dashes]]))

(defmacro apply-middleware [base-tokenizer & middleware]
  `(clojure.core/-> ~base-tokenizer ~@middleware))

(defn split-regex [regex]
  (fn [w]
    (str/split w regex)))

(defn apply-local-regexes [tokens & regexes]
  (loop [regexes regexes
         tokens tokens]
    (if-not (next regexes)
      tokens
      (recur (next regexes) (mapcat (split-regex (first regexes)) tokens)))))

(def regexes
  {:brackets #"((?=[\]\[])|(?<=[\[\]]))"
   :parens #"((?=[\)\(])|(?<=[\(\)]))"})

(defn regexes-middleware
  "middleware to split non-tokenized words containing parens"
  [tokenizer & regexes]
  (fn [sent]
    (let [tokens (tokenizer sent)]
      (apply apply-local-regexes tokens regexes))))

(defn dash-middleware
  [tokenizer lexicon]
  (fn [sent]
    (let [tokens (tokenizer sent)]
      (mapcat (fn [token]
                (cond (not (has-dash token) token)
                      (contains? lexicon (str/lower-case token)) token
                      :else (split-dashes token)))
              tokens))))

(comment (let [tokenizer #(str/split % #" ")]
           (-> tokenizer
               (regexes-middleware (:brackets regexes) (:parens regexes))
               (dash-middleware (read-lexicon "dash-lexicon.txt")))))
