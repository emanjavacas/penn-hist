(ns penn-hist.tokenizer
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [penn-hist.readers :refer [string-pos-sents]]
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

(defn tokenize-file [infn outfn tokenizer]
  (with-open [rdr (clojure.java.io/reader infn)
              wrt (clojure.java.io/writer outfn)]
    (log/info "Tokenizing " (.getName (io/as-file infn)) " to " (.getName (io/as-file outfn)))
    (doseq [line (line-seq rdr)
            :let [tokens (tokenizer line)
                  out-ln (str (str/join " " tokens) "\n")]]
      (.write wrt out-ln))))

(defn make-tokenizer [modelfn]
  (m/wrap-tokenizer (nlp/make-tokenizer modelfn)))

(defn run-tokenizer-on-file [infn outfn tokenizer]
  (with-open [rdr (clojure.java.io/reader infn)
              wrt (clojure.java.io/writer outfn)]
    (doseq [line (line-seq rdr)
            :let [tokens (tokenizer line)]]
      (.write wrt (str (str/join " " tokens) "\n")))))

(defmulti run-tokenizer
  "tokenizes infn into outfn using modelfn"
  (fn [inpath outpath modelfn]
    (let [infile (clojure.java.io/file inpath)]
      (cond (.isDirectory infile) :dir
            (.isFile infile)      :file
            :else                 (throw (ex-info "Input path doesn't exist" {:path inpath}))))))

(defmethod run-tokenizer :file
  [inpath outpath modelfn]
  (let [tokenizer (make-tokenizer modelfn)]
    (run-tokenizer-on-file inpath outpath tokenizer)))

(defmethod run-tokenizer :dir
  [inpath outpath modelfn]
  (let [tokenizer (make-tokenizer modelfn)
        files (file-seq (clojure.java.io/file inpath))]
    (doseq [f files
            :when (.isFile f)
            :let [outpath (str outpath "/" (.getName f) ".token")]]
      (log/info (str "Tokenizing file: [" outpath "]" ))
      (run-tokenizer-on-file f outpath tokenizer))))

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
        "  tokenize Tokenize text.     Requires infn, outfn & modelfn"]
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
  (let [cli-options [["-n" "--n-sents N-SENTS" "Number of sentences to train on (train)"
                      :parse-fn #(Integer/parseInt %)]
                     ["-o" "--outfn OUTPUT-FILE" "path to the output file (train/tokenize)"
                      :parse-fn str]
                     ["-i" "--infn INPUT-FILE" "path to the input file (tokenize)"]
                     ["-m" "--modelfn MODEL-PATH" "path to model binaries (tokenize)"]
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
              "help" (usage summary)
              (usage summary)))))
