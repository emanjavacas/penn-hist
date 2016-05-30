(ns penn-hist.middleware
  (:require [clojure.string :as str]
            [penn-hist.build-lexicon :refer [read-lexicon has-dash split-dashes]]))

(def begin-parens  "(?=[\\(\\)])")
(def begin-bracket "(?=[\\[\\]])")
(def end-parens "(?<=[\\(\\)])")
(def end-bracket "(?<=[\\[\\]])")

(defn merge-regexes [& groups]
  (re-pattern (str "(" (str/join "|" groups) ")")))

(defn split-middleware
  [tokenizer]
  (let [regex (merge-regexes begin-parens begin-bracket end-parens end-bracket)]
    (fn [sent]
      (->> (tokenizer sent)
           (mapcat #(str/split % regex))))))

(defn dash-middleware
  [tokenizer lexicon]
  (fn [sent]
    (->> (tokenizer sent)
         (mapcat (fn [token]
                   (cond (not (has-dash token)) [token]
                         (contains? lexicon (str/lower-case token)) [token]
                         :else (split-dashes token)))))))

(defn wrap-tokenizer [tokenizer]
  (-> tokenizer
      split-middleware
      (dash-middleware (read-lexicon "dash-tokens.txt"))))


