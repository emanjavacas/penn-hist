(ns penn-hist.readers
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.csv :as csv]
            [environ.core :refer [env]]))

(def root (:root env))

(defn get-info
  "reads the corpus info file"
  [& {:keys [in-file] :or {in-file (str root "corpus_data.csv")}}]
  (prn root)
  (with-open [file (io/reader in-file)]
    (let [lines (next (doall (csv/read-csv file)))
          ->row (fn [& keys] (fn [l] (zipmap keys (next l))))]
      (zipmap (map first lines)
              (map (->row :date :genre :wordcount :simple-genre :span)
                   lines)))))

(defn files
  "a lazy-seq over files matching a given regex"
  [root & {:keys [regex] :or {regex #"\.pos$"}}]
  (let [dir? (fn [f] (.isDirectory f))
        list-files (fn [dir] (.listFiles dir))]
    (filter (fn [f] (and (not (dir? f)) (re-find regex (.getName f))))
            (tree-seq dir? list-files (io/file root)))))

(defn lines
  "lazy-seq over lines from a given set of files"
  [root]
  (let [fs (files root)]
    (mapcat (fn [f] (line-seq (io/reader f))) fs)))

;;; Part of Speech
(defn remove-corpus-meta
  "drops lines containing corpus metadata: '^[<|{].*'"
  [lines]
  (filter (fn [line] (not (re-matches #"^[<|{].*" line))) lines))

(defn merge-pos-lines
  "collapses a seq of words (as in the pos-format) into a seq of sents"
  [lines]
  (partition-by empty? lines))

(defn remove-empty-lines
  "removes empty lines"
  [lines]
  (remove (partial some empty?) lines))

(defn split-pos-token
  "splits word/tag tokens into [word tag] vectors"
  [line]
  (mapv #(str/split % #"/") line))

(defn add-sentence-meta
  "incorporates sent-id as metadata into each line"
  [sent]
  (let [[id tag] (last sent)]
    (assert (= tag "ID"))
    (with-meta (butlast sent) {:sent-id (.toLowerCase id)})))

(defn parse-pos-lines
  "main parsing function for POS"
  [lines]
  (->> lines
       remove-corpus-meta
       merge-pos-lines
       remove-empty-lines
       (map split-pos-token)       
       (map add-sentence-meta)))

(defn get-file-from-meta
  "extracts the name of the file to which the sent belongs"
  [sent]
  (if-let [id (:sent-id (meta sent))]
    (first (str/split id #","))))

(defn pos-sent->string-sent
  "joins a seq of [word tag] vectors into a string sentence"
  [token-sent]
  (str/join " " (map first token-sent)))

(def pos-sents (parse-pos-lines (lines root)))
(def string-pos-sents (map pos-sent->string-sent pos-sents))
