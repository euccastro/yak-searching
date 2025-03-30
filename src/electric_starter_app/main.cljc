(ns electric-starter-app.main
  (:require #?(:clj [electric-starter-app.debug :refer [pprint-str]])
            [electric-starter-app.mock :as mock]
            [electric-starter-app.model :as model]
            [electric-starter-app.simplify-filters :refer [simplify-filters]]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [hyperfiddle.electric-forms5 :as form]))

(defn px [n]
  (str n "px"))

(def table-border "1px solid lightgray")

(def cell-padding 8)

(defn width [col-width])

(e/defn On [evt Cb & args]
  (e/client
    (when-some [t (let [e (dom/On evt identity nil)
                        [t _] (e/Token e)]
                    t)]
      (case (e/apply Cb args)
        (do (t) (e/amb))))))

(def row-height 24)

(def table-rows 10)

(e/defn Table [types
               type-k
               items
               HeaderDoubleClickAction
               Row]
  (let [{:keys [attributes] :as _type} (get types type-k)]
    (dom/div
      (dom/style (dom/text form/css))
      ;; Total height better be an exact multiple of row height. I
      ;; found this fixed an issue where every screen two consecutive
      ;; rows would have a grey background.
      (dom/props {:style {:height (px (* row-height table-rows))
                          ;; set this or table cells will be stacked vertically
                          :--column-count (count attributes)}})
      ;; XXX: add back header.
      ;; XXX: add back column widths, at least as min-width
      (form/VirtualScroll :table :tr row-height 1 items Row))))

(e/defn Row [DisplayCell types type-k OnClick _index row]
  (e/server
    (let [{:keys [attributes pk] :as _type} (get types type-k)]
      (On "click" OnClick (pk row))
      (e/for [col (e/diff-by identity attributes)]
        (dom/td
          (dom/text (DisplayCell (:type col) (get row (:k col)))))))))

;; XXX: allow initializing with some filters?
;; XXX: take callback?
(e/defn YakSearch* [Search DisplayCell types !stack]
  ;; TODO: breadcrumb view
  ;; TODO: filters input
  (e/server
    (let [stack (e/watch !stack)
          {:keys [title type-k filters cart Cb]} (last stack)
          {:keys [pk] :as _type} (get types type-k)]
      (dom/pre (dom/text (pprint-str stack)))
      (dom/h3 (dom/text "Options"))
      (Table types
             type-k
             (Search type-k (map simplify-filters filters))
             (e/fn [attr]
               (e/server
                 (println "DBLCLK" (pprint-str attr))
                 (swap! !stack
                        conj
                        {:title (str "Choose " (:label attr))
                         :type-k (:type attr)
                         :filters []
                         :cart #{}
                         :Cb (e/fn [selections]
                               (e/server
                                 (swap! !stack
                                        (fn [st]
                                          (-> st
                                              (update-in [(-> st count dec dec)
                                                          :filters]
                                                         conj
                                                         [:in (:k attr) selections])
                                              butlast
                                              vec)))
                                 nil))})
                 nil))
             (e/Partial Row
                        DisplayCell
                        types
                        type-k
                        (e/fn [id]
                          (e/server
                            (do (swap! !stack
                                       (fn [st]
                                         (update-in st [(dec (count st)) :cart]
                                                    conj id)))
                                nil)))))
      (dom/h3 (dom/text "Selection"))
      (Table types
             type-k
             (when (seq cart) (Search type-k [[:in pk cart]]))
             (e/fn [_] nil)
             (e/Partial Row
                        DisplayCell
                        types
                        type-k
                        (e/fn [id]
                          (e/server
                            (do (swap! !stack
                                       (fn [st]
                                         (update-in st [(dec (count st)) :cart]
                                                    disj id)))
                                (e/amb))))))
      (dom/button
        (On "click" Cb cart)
        (dom/text "Confirm"))
      (dom/button
        (On "click" Cb nil)
        (dom/text "Cancel"))))
  ;; TODO: Submit button (and take callback? return (e/amb) while not selected?)
  )

(e/defn YakSearch [Search DisplayCell types title type-k Cb]
  (e/server
    (YakSearch* Search
                DisplayCell
                types
                (atom [{:title title
                        :type-k type-k
                        :filters []
                        :cart #{}
                        :Cb Cb}]))))

(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
      (dom/div (dom/props {:style {:display "contents"}})
               (dom/h1 (dom/text "Hello!"))
               (e/server
                (YakSearch mock/Search
                           mock/DisplayCell
                           model/types
                           "Choose task"
                           :task
                           (e/fn [choices]
                             (e/server (println "CHOSE" (pr-str choices))))))))))
