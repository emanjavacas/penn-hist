(ns penn-hist.readers
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io StringReader]))

(def root "/Users/quique/corpora/PENN-CORPORA/")

(defn files [root & {:keys [regex] :or {regex #"\.pos$"}}]
  (let [dir? (fn [f] (.isDirectory f))
        list-files (fn [dir] (.listFiles dir))]
    (filter (fn [f] (and (not (dir? f)) (re-find regex f)))
            (tree-seq dir? list-files (io/file root)))))

(defn lines [root]
  (let [fs (files root)]
    (mapcat (fn [f] (line-seq (io/reader f))) fs)))

(defn add-sentence-meta [sent]
  (let [id (last sent)]
    (with-meta (butlast sent) {:sent-id id})))

(defn parse-pos-lines [lines]
  (let [filtered-meta (filter #(not (re-matches #"^[<|{].*" %)) lines)
        partitioned (partition-by empty? filtered-meta)
        removed-empty (filter (partial some (comp not empty?)) partitioned)]
    (map add-sentence-meta removed-empty)))

(defn split-pos-token [parsed-pos-line]
  (map #(str/split % #"/") parsed-pos-line))

(defn to-long-string [token-sents]
  (map (partial map first) token-sents))

;; (str/join " " ["a" "b"])
;; (first (to-long-string (map split-pos-token (parse-pos-lines (lines root)))))
