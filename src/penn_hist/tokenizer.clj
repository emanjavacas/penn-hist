(ns penn-hist.tokenizer
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [penn-hist.middleware :as m]
            [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]))

(def SPLIT "<SPLIT>")

(defn string-sent->maxent
  "inserts <SPLIT> token in front blankspace followed by punctuation"
  [sent]
  (str/replace sent #" ([^ \w]+)" (str SPLIT "$1")))

(defn train-token-model
  "trains tokenization model"
  [string-pos-sents]
  (let [split-sents (map string-sent->maxent string-pos-sents)
        joint-sents (str/join "\n" split-sents)]
    (train/train-tokenizer
     (java.io.BufferedReader. (java.io.StringReader. joint-sents)))))

(defn save-trained-model [outfn model]
  (train/write-model model outfn))

(defn tokenize-file [infn outfn tokenizer]
  (with-open [rdr (io/reader infn)
              wrt (io/writer outfn)]
    (doseq [line (line-seq rdr)
            :let [tokens (tokenizer line)]]
      (.write wrt (str (str/join " " tokens) "\n")))))

(defn make-tokenizer [modelfn]
  (m/wrap-tokenizer (nlp/make-tokenizer modelfn)))



