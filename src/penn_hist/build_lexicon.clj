(ns penn-hist.build-lexicon
  (:require [penn-hist.readers :refer [pos-sents]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn has-dash [s & {:keys [dashes] :or {dashes "—-"}}]
  (re-matches (re-pattern (str "\\w+([" dashes "]\\w+)+")) s))

(defn split-dashes [token & {:keys [dashes] :or {dashes "—-"}}]
  (str/split token (re-pattern (str "((?=[" dashes "])|(?<=[" dashes "]))"))))

(comment (split-dashes "Iam-fun"))

(def tokens (mapcat #(map first %) pos-sents))
(def dash-tokens (filter has-dash tokens))

(defn path-to [dir fname]
  (str/join "/" [(System/getProperty "user.dir") dir fname]))

(defn write-tokens [tokens fname & {:keys [sep] :or {sep " "}}]
  (let [out-file (path-to "resources" fname)]
    (with-open [wrt (io/writer out-file)]
      (doseq [[w freq] (sort-by second > (frequencies tokens))]
        (.write wrt (str w sep freq "\n"))))))

(comment (write-tokens dash-tokens "dash-tokens.txt"))
(comment (write-tokens tokens "token-frequencies.txt"))

(defn read-lexicon
  [fname & {:keys [sep on-key-fn] :or {sep " " on-key-fn str/lower-case}}]
  (with-open [rdr (io/reader (path-to "resources" fname))]
    (into {} (doall
              (for [line (line-seq rdr)
                    :let [[w freq] (str/split line (re-pattern sep))]]
                [(on-key-fn w) freq])))))

(comment (def dash-lexicon (read-lexicon "dash-tokens.txt")))
