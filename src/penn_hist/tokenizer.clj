me(ns penn-hist.tokenizer
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [penn-hist.readers :refer [string-pos-sents]]
            [opennlp.nlp :as nlp]
            [opennlp.tools.train :as train]))

(defn string-sent->maxent
  "inserts <SPLIT> token in front blankspace followed by punctuation"
  [sent]
  (str/replace sent #" ([^ \w]+)" "<SPLIT>$1"))

(defn train-token-model
  "trains tokenization model"
  [string-pos-sents]
  (let [split-sents (map string-sent->maxent string-pos-sents)
        joint-sents (str/join "\n" split-sents)]
    (train/train-tokenizer
     (java.io.BufferedReader. (java.io.StringReader. joint-sents)))))

(defmacro apply-middleware [base-tokenizer & middleware-fns]
  `(-> ~base-tokenizer ~@middleware-fns))

(defn split-parens
  "middleware to split non-tokenized words containing parens"
  [tokenizer]
  (fn [sent]
    (let [tokens (tokenizer sent)]
      (mapcat (fn [w]
                (str/split w #"((?=\))|(?<=\())"))
              tokens))))

(defn make-tokenizer [modelfn & middleware]
  (let [tokenizer (nlp/make-tokenizer modelfn)]
    (if (empty? middleware)
      tokenizer
      (apply-middleware tokenizer middleware))))

(defn run-tokenizer
  "tokenizes infn into outfn using modelfn"
  [infn outfn modelfn & middleware]
  {:pre [(and infn outfn modelfn)]}
  (let [tokenizer (make-tokenizer modelfn split-parens)]
    (with-open [rdr (clojure.java.io/reader infn)
                wrt (clojure.java.io/writer outfn)]
      (doseq [line (line-seq rdr)
              :let [tokens (tokenizer line)
                    out-ln (str (str/join " " tokens) "\n")]]
        (.write wrt out-ln)))))

(defn usage [options-summary]
  (->> ["Train or use a OpenNLP tokenizer through Clojure"
        ""
        "Usage: lein run action [options]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  train    Train a tokenizer. Requires n-sents & outfn (for the model)"
        "  tokenize Tokenize text.     Requires infn, outfn & modelfn"
        ""
        "Please refer to the manual page for more information."]
       (str/join \newline)))

(defn ensure-ext [fname ext]
  (if-not (.endsWith fname ext)
    (str fname ext)
    fname))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [cli-options [["-n" "--n-sents N-SENTS" "Number of sentences to train on"
                      :parse-fn #(Integer/parseInt %)]
                     ["-o" "--outfn OUTPUT-FILE" "path to the output file"               
                      :parse-fn str]
                     ["-i" "--infn INPUT-FILE" "path to the input file"]
                     ["-m" "--modelfn MODEL-PATH" "path to model binaries"]
                     ["-h" "--help"]]
        {:keys [options arguments errors summary]} (parse-opts args cli-options)
        {:keys [tokenize train n-sents outfn infn modelfn help]} options]
    (cond
      help (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors))
      :else (case (first arguments)
              "tokenize" (run-tokenizer infn outfn modelfn)
              "train" (let [model (train-token-model (take n-sents string-pos-sents))
                            fname (ensure-ext outfn "bin")]
                        (log/info "Saving model to" outfn)
                        (train/write-model model fname))
              (throw (ex-info "Don't know what to do" {:cause "Unknown clause"}))))))
