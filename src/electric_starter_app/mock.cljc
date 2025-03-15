(ns electric-starter-app.mock
  (:require [electric-starter-app.model :as model]
            [hyperfiddle.electric3 :as e]
            [medley.core :as md]))

(def data
  {:user [{:id 1 :name "John"}
          {:id 2 :name "Mary"}]
   :task [{:id 1 :assignee 1 :title "Buy milk" :creator 2 :status :done}
          {:id 2 :assignee 2 :title "Shave yak" :creator 1 :status :pending}]})

(defn find-by-pk [type pk-val]
  (if-let [items (get data type)]
    (let [{:keys [pk]} (get model/types type)]
      (md/find-first #(= (pk %) pk-val)
                     items))
    pk-val))

(defn compile-filter [[discriminator & args]]
  (case discriminator
    :not (do
           (assert (= (count args) 1))
           (complement (compile-filter (first args))))
    :in (let [[k xs] args
              xss (set xs)]
          #(contains? xss (get % k)))
    := #(apply = (map (fn [x]
                        (if (keyword? x)
                          (get % x)
                          x))
                      args))))

(defn search [type-k filters]
  (prn "search" type-k filters)
  (reduce (fn [vals f]
            (filter f vals))
          (data type-k)
          (map compile-filter filters)))

(e/defn Search [type-k filters #_order #_offset #_limit]
  (search type-k filters))

(defn display [type-k item-k]
  ((-> model/types type-k :display) (find-by-pk type-k item-k)))

(e/defn DisplayCell [type-k item-k]
  (display type-k item-k))
