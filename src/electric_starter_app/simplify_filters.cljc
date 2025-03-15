(ns electric-starter-app.simplify-filters
  (:require [clojure.set :as set]
            [clojure.walk :refer [postwalk]]))

(def pass-unconditionally [:= 1 1])
(def fail-unconditionally [:= 0 1])

(defn- trivial-negations [expr]
  (get
   {[:not pass-unconditionally] fail-unconditionally
    [:not fail-unconditionally] pass-unconditionally}
   expr
   expr))

(defn- remove-unconditionals-from-and [expr]
  (if (and (sequential? expr) (= (first expr) :and))
    (if (some #{fail-unconditionally} expr)
      fail-unconditionally
      (vec (remove #{pass-unconditionally} expr)))
    expr))

(defn- remove-unconditionals-from-or [expr]
  (if (and (sequential? expr) (= (first expr) :or))
    (if (some #{pass-unconditionally} expr)
      pass-unconditionally
      (vec (remove #{fail-unconditionally} expr)))
    expr))

(defn- empty-and [expr]
  (if (= expr [:and])
    pass-unconditionally
    expr))

(defn- empty-or [expr]
  (if (= expr [:or])
    fail-unconditionally
    expr))

(defn- unary-and-or [expr]
  (if (and (sequential? expr)
           (#{:and :or} (first expr))
           (= (count expr) 2))
    (second expr)
    expr))

(defn- ?first [x]
  (when (seqable? x) (first x)))

(defn- merge-=-in
  "Merge and simplify = and IN expressions."
  [x]
  (let [op (?first x)
        set-comp (case op
                   :or set/union
                   :and set/intersection
                   nil)]
    (if-not set-comp
      x
      (let [clauses (rest x)
            {=s [true false]
             ins [false true]
             others [false false]}
            (group-by (juxt #(= (?first %) :=)
                            #(= (?first %) :in))
                      clauses)
            in-groups (group-by second ins)
            =groups (group-by second =s)
            groups (for [k (set (concat (keys in-groups) (keys =groups)))]
                     [k (apply set-comp
                               (concat
                                (map #(set [(nth % 2)]) (=groups k))
                                (map #(nth % 2) (in-groups k))))])
            {multiple true single false}
            (group-by #(> (count (second %)) 1) groups)]
        (into [op]
              (concat others
                      (for [[k xs] single] [:= k (first xs)])
                      (for [[k xs] multiple] [:in k xs])))))))

(defn- fail-in-empty [x]
  (if (and (= (?first x) :in)
           (empty? (nth x 2)))
    fail-unconditionally
    x))

(comment
  (merge-=-in [:or
               [:in
                :od.object_type
                #{"IRRADIATION_SENSOR" "SENSOR_BOX"}]
               [:in :something.else "SOMETHING_OTHER"]
               [:= :od.object_type "INVERTER"]])
  ;; =>
  [:or
   [:in :something.else "SOMETHING_OTHER"]
   [:in :od.object_type #{"IRRADIATION_SENSOR" "INVERTER" "SENSOR_BOX"}]]

  (merge-=-in [:and
               [:in
                :od.object_type
                #{"IRRADIATION_SENSOR" "SENSOR_BOX" "METEO_STATION" "SENSOR"
                  "COMPUTED_SENSOR" "INVERTER"}]
               [:= :od.object_type "INVERTER"]])
  ;; =>
  [:and [:= :od.object_type "INVERTER"]])

(def ^:private simplify-step
  (apply comp
         (map (partial partial postwalk)
              [trivial-negations
               remove-unconditionals-from-and
               remove-unconditionals-from-or
               empty-and
               empty-or
               unary-and-or
               merge-=-in
               fail-in-empty])))

(defn- fixed-point [f]
  #(->> %
        (iterate f)
        (partition 2)
        (some (fn [[prev next]]
                (when (= prev next)
                  prev)))))

(def simplify-filters (fixed-point simplify-step))

(comment

  (simplify-filters
   [:and [:not [:in :id #{}]]])
  )
