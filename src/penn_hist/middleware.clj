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
  "lexicon is a ma"
  [tokenizer lexicon]
  (fn [sent]
    (->> (tokenizer sent)
         (mapcat (fn [token]
                   (cond (not (has-dash token)) [token]
                         (contains? lexicon (str/lower-case token)) [token]
                         :else (split-dashes token)))))))

(comment
  (def test-sents
  [["Thus spoke the Matron—The convicted Crew"
    "Thus spoke the Matron — The convicted Crew"]
   ["A Duke, a Dog-whipper you are! such a knot of Fools, that the King, instead of punishing,"
    "A Duke , a Dog-whipper you are ! such a knot of Fools , that the King , instead of punishing ,"]
   ["pities you—But I shall make bold to turn you out of your Dignitie, my Lord Duke."
    "pities you — But I shall make bold to turn you out of your Dignitie , my Lord Duke ."]
   ["should have been [All those] when as there is never a [Those] at all in the argument ."
    "should have been [ All those ] when as there is never a [ Those ] at all in the argument ."]])
  (def tokenizer
    (-> #(str/split % #" ")
        (regexes-middleware (:brackets regexes) (:parens regexes))
        (dash-middleware (read-lexicon "dash-tokens.txt"))))
  (tokenizer (first (second test-sents))))
