(ns penn-hist.core-test
  (:require [clojure.test :refer :all]
            [clojure.string :refer [join]]
            [penn-hist.tokenizer :refer [make-tokenizer]]))

(def tokenizer (make-tokenizer "resources/models/tokenizer_model_all.bin"))

(deftest test-dash
  (is (= (join " " (tokenizer "Thus spoke the Matron—The convicted Crew"))
         "Thus spoke the Matron — The convicted Crew"))
  (is (= (tokenizer "pities you—But I shall make bold to turn you out of your Dignitie,"))
          "pities you — But I shall make bold to turn you out of your Dignitie ,"))

(deftest test-parens
  (is (= (join " " (tokenizer "Such Prayers, (though starting last) will come first to the Mark."))
         "Such Prayers , ( though starting last ) will come first to the Mark .")))

(deftest test-exclamation
  (is (= (join " " (tokenizer "A Duke, a Dog-whipper you are! such a knot of Fools, that the King,"))
         "A Duke , a Dog - whipper you are ! such a knot of Fools , that the King ,")))

(deftest test-brackets
  (is (= (join " " (tokenizer "should have been [All those] when as there is never a [Those] at all"))
         "should have been [ All those ] when as there is never a [ Those ] at all")))
