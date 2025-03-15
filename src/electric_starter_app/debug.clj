(ns electric-starter-app.debug
  (:require [clojure.pprint :refer [pprint]])
  (:import [java.io StringWriter]))

(set! *warn-on-reflection* true)

(defn pprint-str [x]
  (let [writer (StringWriter.)]
    (clojure.pprint/pprint x writer)
    (.toString writer)))
