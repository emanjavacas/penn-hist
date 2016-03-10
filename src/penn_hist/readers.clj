(ns penn-hist.readers
  (:require [clojure.java.io :as io]
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
