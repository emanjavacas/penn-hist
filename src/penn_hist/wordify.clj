(ns penn-hist.wordify
  (:require [clojure.zip :as zip]
            [clojure.data.xml :as xml]))

(defonce counter (atom 0))

(defn depth-first [loc]
  (take-while (complement zip/end?) (iterate zip/next loc)))

(defn find-by-tag [tag zipper]
  (filter #(= tag (:tag (zip/node %))) (depth-first zipper)))

(defn edit-tree
  "Take a zipper, a function that matches a pattern in the tree,
   and a function that edits the current location in the tree.
   Examine the tree nodes in depth-first order."
  [zipper editor & {:keys [matcher] :or {matcher identity}}]
  (loop [loc zipper]
    (if (zip/end? loc)
      (zip/root loc)
      (if (matcher loc)
        (let [new-loc (zip/edit loc editor loc)]
          (recur (zip/next new-loc)))
        (recur (zip/next loc))))))

(defn- dummy-free-editor
  "removes :dummy elements introduced by `words-editor`"
  [node _]
  (if-not (string? node)
    (let [content (:content node)
          new-content (mapcat (fn [n] (if (= :dummy (:tag n)) (:content n) [n])) content)]
      (assoc node :content new-content))
    node))

(defn- remove-dummies
  "returns a xml tree without :dummy elements as per `dummy-free-editor`"
  [zipper]
  (edit-tree zipper dummy-free-editor))

(defn match-tag [tag]
  (fn [loc]
    (if-let [node (zip/node loc)]
      (= tag (:tag node))
      false)))

(def match-word (match-tag :w))

(defn words-editor
  "returns an editor that converts textual nodes into a tokenized sequence of :w nodes. Leaves
   residual :dummy nodes as parents of each seq of :w tokens. Use `remove-dummies` for that."
  [tokenizer include-ids]
  (fn [node loc]
    (let [parent (zip/up loc)]
      (if (and parent (not (match-word parent)) (string? node))
        (apply xml/element :dummy {}
               (mapv (fn [w] (let [attrib (if include-ids {:id (swap! counter inc)} {})]
                               (xml/element :w attrib w)))
                     (remove empty? (tokenizer node))))
        node))))

(defn wordify
  "returns a xml tree with tokenized words inside :w elements with consecutive ids"
  [zipper tokenizer include-ids]
  (->> (edit-tree zipper (words-editor tokenizer include-ids)) zip/xml-zip remove-dummies))

(defn wordify-editor
  [tokenizer {:keys [include-ids]}]
  (fn [node loc]
    (let [zipper (zip/xml-zip node)]
      (zip/xml-zip (wordify zipper tokenizer include-ids)))))

(defn f->zip [fname]
  (-> fname
      clojure.java.io/input-stream
      xml/parse
      zip/xml-zip))

(defn xml->file [tree outfn]
  (spit outfn (xml/indent-str tree)))

(defn wordify-file
  [infn outfn tokenizer & {:keys [include-ids tag] :or {include-ids true match-tag :doc} :as args}]
  (-> (f->zip infn)
      (edit-tree (wordify-editor tokenizer args) :matcher (match-tag tag))
      (xml->file outfn)))

(comment (wordify-file
          "/home/enrique/mbg_repos/mbg_corpus/output/A00214.xml"
          "./dev-resources/test.out.xml"
          (penn-hist.tokenizer/make-tokenizer "resources/models/tokenizer_model_all.bin")))
