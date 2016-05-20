(ns penn-hist.psd
  (:require [penn-hist.readers :refer [files lines]]
            [clojure.string :as str]
            [environ.core :refer [env]]))

(def root (:root env))

(defn strip-comments [s]
  )
