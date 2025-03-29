(ns electric-starter-app.util
  #?(:cljs (:require-macros electric-starter-app.util)))


(defmacro do1 [x & body] `(let [ret# ~x] ~@body ret#))
