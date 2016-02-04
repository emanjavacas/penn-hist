(ns penn-hist.tokenizer
  (:require [penn-hist.readers :refer [take-string-sents]])
  (:import [opennlp.tools.tokenize TokenizerME TokenizerModel]
           [opennlp.tools.util PlainTextByLineStream TrainingParameters]
           [opennlp.tools.tokenize TokenSampleStream]))

(defn ^TokenizerModel train-tokenizer
  "Returns a tokenizer based on given training file"
  ([in] (train-tokenizer "en" in))
  ([lang in] (train-tokenizer lang in 100 5))
  ([lang in iter cut]
     (with-open [rdr (clojure.java.io/reader in)]
       (TokenizerME/train
        lang
        (->> rdr
             (PlainTextByLineStream.)
             (TokenSampleStream.))
        false
        cut
        iter))))

(def tokenizer
  "not working :-("
  (train-tokenizer
   (java.io.BufferedReader. (java.io.StringReader. (take-string-sents 5000)))))
