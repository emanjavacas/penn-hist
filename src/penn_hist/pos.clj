(ns penn-hist.pos
  (:require [penn-hist.readers :refer [get-info files lazy-lines]]
            [clojure.string :as str]
            [environ.core :refer [env]]))



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
