(defproject penn-hist "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [clojure-opennlp "0.3.3"]
                 [org.clojure/data.csv "0.1.3"]
                 [org.clojure/tools.cli "0.3.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [environ "1.0.2"]]
  :plugins [[lein-environ "1.0.2"]]
  :jvm-opts ["-Xmx4000M"]
  :main "penn-hist.tokenizer"
  :profiles {:user {:env {:root "/home/enrique/corpora/PENN-CORPORA/"}}})
