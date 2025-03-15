(ns electric-starter-app.main
  (:require #?(:clj [electric-starter-app.debug :refer [pprint-str]])
            [electric-starter-app.mock :as mock]
            [electric-starter-app.model :as model]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]))

;; XXX state é stack de seleçons, só mostramos a última
;; XXX for odd/even highlighting, even with infinite scroll, track parity of
;; absolute offset

(defn px [n]
  (str n "px"))

(def table-border "1px solid lightgray")

(def cell-padding 8)

(defn width [col-width])

(e/defn Table [DisplayCell types type-k items button-label ButtonAction]
  (let [{:keys [attributes pk] :as _type} (get types type-k)]
    (dom/table
      (dom/props {:style {:border table-border
                          :table-layout "fixed"
                          :border-collapse "collapse"
                          :width (px (reduce +
                                             100
                                             (map :width attributes)))}})
      (dom/thead
        (dom/tr
          (e/amb
           (e/for [col (e/diff-by identity attributes)]
             (dom/th (dom/props {:style {:width (-> col :width px)
                                         :border-right table-border}})
                     (dom/text (:label col))))
           (dom/th (dom/text " ")))))
      (dom/tbody
        (e/for [row (e/diff-by identity items)]
          (dom/tr
            (e/amb
             (e/for [col (e/diff-by identity attributes)]
               (dom/td
                 (dom/props {:style {:width (-> col :width px)
                                     :border-top table-border
                                     :border-right table-border}})
                 (dom/text (DisplayCell (:type col) (get row (:k col))))))
             (dom/td
               (dom/props {:style {:width 100
                                   :border-top table-border}})
               (e/client
                 (when-some [t (dom/button (dom/text button-label)
                                           (let [e (dom/On "click" identity nil)
                                                 [t _] (e/Token e)]
                                             t))]
                   (case (ButtonAction (pk row)) (do (t) (e/amb)))))))))))))

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
      (Table DisplayCell
             types
             type-k
             (Search type-k (when (seq cart) [[:not [:in pk cart]]]))
             "Add"
             (e/fn [id]
               (e/server
                 (do (swap! !stack
                            (fn [st]
                              (update-in st [(dec (count st)) :cart]
                                         conj id)))
                     (e/amb)))))
      (dom/h3 (dom/text "Selection"))
      (Table DisplayCell
             types
             type-k
             (when (seq cart) (Search type-k [[:in pk cart]]))
             "Remove"
             (e/fn [id]
               (e/server
                 (do (swap! !stack
                            (fn [st]
                              (update-in st [(dec (count st)) :cart]
                                         conj id)))
                     (e/amb)))))))
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
               (e/server
                (YakSearch mock/Search
                           mock/DisplayCell
                           model/types
                           "Choose task"
                           :task
                           nil))))))
