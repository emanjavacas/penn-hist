(ns penn-hist.main
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [penn-hist.readers :refer [string-pos-sents]]
            [penn-hist.wordify :refer [wordify-file]]
            [penn-hist.tokenizer :refer
             [train-token-model save-trained-model tokenize-file make-tokenizer]])
  (:gen-class))

(defn run-training [infn outfn n-sents]
  (let [model (train-token-model (take n-sents (string-pos-sents infn)))]
    (log/info "Saving model to" outfn)
    (save-trained-model outfn model)))

(defn dispatch-on-type [inpath outpath]
  (let [infile (io/file inpath)]
    (cond (.isDirectory infile) :dir
          (.isFile infile)      :file
          :else                 (throw (ex-info "Input path doesn't exist" {:path inpath})))))

(defn run-on-files [inpath outpath modelfn {:keys [ext on-file-fn info-fn]}]
  (let [tokenizer (make-tokenizer modelfn)
        files (file-seq (io/file inpath))]
    (doseq [f files
            :when (.isFile f)
            :let [inpath (.getAbsolutePath f)
                  basename (first (clojure.string/split (.getName f) #"\.(?=[^\.]+$)"))
                  outpath (str outpath "/" basename "." ext)]]
      (log/info (info-fn outpath))
      (try
        (on-file-fn inpath outpath tokenizer)
        (catch Exception e
          (log/info "Error while processing file [" inpath "]" (str e)))))))

(defmulti run-tokenizer
  "tokenizes infn into outfn using modelfn"
  (fn [inpath outpath modelfn] (dispatch-on-type inpath outpath)))

(defmethod run-tokenizer :file
  [inpath outpath modelfn]
  (let [tokenizer (make-tokenizer modelfn)]
    (tokenize-file inpath outpath tokenizer)))

(defmethod run-tokenizer :dir
  [inpath outpath modelfn]
  (run-on-files
   inpath outpath modelfn
   {:on-file-fn tokenize-file
    :ext "tokens"
    :info-fn (fn [outpath] (str "Tokenizing file: [" outpath "]"))}))

(defmulti wordify
  (fn [inpath outpath modelfn] (dispatch-on-type inpath outpath)))

(defmethod wordify :file
  [inpath outpath modelfn]
  (let [tokenizer (make-tokenizer modelfn)]
    (wordify-file inpath outpath tokenizer)))

(defmethod wordify :dir
  [inpath outpath modelfn]
  (run-on-files
   inpath outpath modelfn
   {:on-file-fn wordify-file
    :ext "xml"
    :info-fn (fn [outpath] (str "Wordifying file: [" outpath "}"))}))

(defn usage [options-summary]
  (->> ["Train or use a OpenNLP tokenizer through Clojure"
        ""
        "Usage: lein run action [options]"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  train    Train a tokenizer.            Requires n-sents & outfn (for the model)"
        "  tokenize Tokenize text.                Requires infn, outfn & modelfn"
        "  wordify  Convert to <w id=INT> format  Requires infn, outfn & modelfn"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main [& args]
  (let [cli-options
        [["-n" "--n-sents N-SENTS" "Number of sentences to train on (train)"
          :parse-fn #(Integer/parseInt %)]
         ["-o" "--outfn OUTPUT-FILE" "path to the output file (train/tokenize)"
          :parse-fn str]
         ["-i" "--infn INPUT-FILE" "path to PENN root/path to the input file (train/tokenize)"]
         ["-m" "--modelfn MODEL-PATH" "path to model binaries (tokenize)"]
         ["-I" "--include-ids INCLUDE_IDS" "append a unique integer id to each word"
          :parse-fn boolean]
         ["-h" "--help"]]
        {:keys [options arguments errors summary]} (parse-opts args cli-options)
        {:keys [tokenize train n-sents outfn infn modelfn help include-ids]} options]
    (cond
      help (exit 0 (usage summary))
      (not= (count arguments) 1) (exit 1 (usage summary))
      errors (exit 1 (error-msg errors))
      :else (case (first arguments)
              "tokenize" (run-tokenizer infn outfn modelfn)
              "train"  (run-training infn outfn n-sents)
              "wordify" (wordify infn outfn modelfn :include-ids include-ids)
              "help" (usage summary)
              (usage summary)))))
