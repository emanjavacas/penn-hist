(ns penn-hist.readers
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.data.csv :as csv])
  (:import [java.io StringReader]))

(def root "/home/enrique/corpora/PENN-CORPORA/")

(defn get-info [& {:keys [in-file]
                   :or {in-file (str root "corpus_data.csv")}}]
  (with-open [file (io/reader in-file)]
    (let [lines (next (doall (csv/read-csv file)))
          ->row (fn [& keys] (fn [l] (zipmap keys (next l))))]
      (zipmap (map first lines)
              (map (->row :date :genre :wordcount :simple-genre :span)
                   lines)))))

(defn files [root & {:keys [regex] :or {regex #"\.pos$"}}]
  (let [dir? (fn [f] (.isDirectory f))
        list-files (fn [dir] (.listFiles dir))]
    (filter (fn [f] (and (not (dir? f)) (re-find regex (.getName f))))
            (tree-seq dir? list-files (io/file root)))))

(defn lines [root]
  (let [fs (files root)]
    (mapcat (fn [f] (line-seq (io/reader f))) fs)))

(defn split-pos-token [parsed-pos-line]
  (map #(str/split % #"/") parsed-pos-line))

(defn add-sentence-meta [sent]
  (let [[id tag] (last sent)]
    (assert (= tag "ID"))
    (with-meta (butlast sent) {:sent-id (.toLowerCase id)})))

(defn parse-pos-lines [lines]
  (->> lines     
       (filter #(not (re-matches #"^[<|{].*" %)))
       (partition-by empty?)
       (remove (partial some empty?))
       (map split-pos-token)       
       (map add-sentence-meta)))

(defn get-file [sent]
  (if-let [id (:sent-id (meta sent))]
    (first (str/split id #","))))

(defn ->string-sent [token-sent]
  (str/join " " (map first token-sent)))

(defn take-string-sents [n]
  (let [string-sents (map ->string-sent (parse-pos-lines (lines root)))]
    (str/join "\n" (take n string-sents))))

(def info (get-info))

;(take-string-sents 2000)
