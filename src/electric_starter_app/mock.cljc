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

(def ingredients-vector
  (let [grains ["flour" "rice" "oats" "cornmeal" "barley" "quinoa" "couscous" "bulgur" "farro" "millet"]
        spices ["salt" "pepper" "cinnamon" "cumin" "paprika" "oregano" "basil" "thyme" "rosemary" "nutmeg"
                "cardamom" "turmeric" "ginger" "cloves" "coriander" "bay leaves" "saffron" "fennel seeds" "star anise" "vanilla"]
        vegetables ["onion" "garlic" "carrot" "potato" "tomato" "bell pepper" "zucchini" "eggplant" "spinach" "kale"
                    "broccoli" "cauliflower" "cabbage" "lettuce" "cucumber" "celery" "mushroom" "corn" "peas" "green beans"]
        fruits ["apple" "banana" "orange" "lemon" "lime" "strawberry" "blueberry" "raspberry" "blackberry" "cherry"
                "peach" "pear" "plum" "grape" "mango" "pineapple" "watermelon" "kiwi" "avocado" "coconut"]
        proteins ["chicken" "beef" "pork" "lamb" "turkey" "tofu" "tempeh" "eggs" "salmon" "tuna"
                  "shrimp" "beans" "lentils" "chickpeas" "nuts" "seeds" "yogurt" "cheese" "milk" "cream"]
        all-ingredients (concat grains spices vegetables fruits proteins)

        grain-weights (range 100 1001 100)       ;; 100g to 1000g
        spice-weights (range 5 101 5)            ;; 5g to 100g
        vegetable-weights (range 50 501 50)      ;; 50g to 500g
        fruit-weights (range 75 751 75)          ;; 75g to 750g
        protein-weights (range 125 1251 125)     ;; 125g to 1250g

        ingredient-weights (fn [ingredient]
                            (cond
                              (some #(= ingredient %) grains) (rand-nth grain-weights)
                              (some #(= ingredient %) spices) (rand-nth spice-weights)
                              (some #(= ingredient %) vegetables) (rand-nth vegetable-weights)
                              (some #(= ingredient %) fruits) (rand-nth fruit-weights)
                              (some #(= ingredient %) proteins) (rand-nth protein-weights)
                              :else 100))
        _ (prn "count" (count all-ingredients))

        selected-ingredients (take 100 (shuffle all-ingredients))]

    (mapv (fn [ingredient]
            {:grams (ingredient-weights ingredient)
             :ingredient ingredient})
          selected-ingredients)))
