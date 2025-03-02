(ns electric-starter-app.main
  (:require [electric-starter-app.mock :as mock]
            [electric-starter-app.model :as model]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]))

(e/defn Table [DisplayCell types type-k items button-label ButtonAction]
  (let [{:keys [attributes pk] :as _type} (get types type-k)]
    (dom/table
     (dom/props {:style {:border "1px solid lightgray"}})
     (dom/thead
      (dom/tr
       (e/for [col (e/diff-by identity attributes)]
         (dom/th (dom/text (:label col))))))
     (dom/tbody
      (e/for [row (e/diff-by identity items)]
        (dom/tr
         (e/amb
          (e/for [col (e/diff-by identity attributes)]
            (dom/td (dom/text (DisplayCell (:type col) (get row (:k col))))))
          (e/client
           (when-some [t (dom/button (dom/text button-label)
                                     (let [e (dom/On "click" identity nil)
                                           [t _] (e/Token e)]
                                       t))]
             (case (ButtonAction (pk row)) (do (t) (e/amb))))))))))))

;; XXX: allow initializing with some filters?
;; XXX: take callback?
(e/defn YakSearch [Search DisplayCell types type-k]
  ;; TODO: breadcrumb view
  ;; TODO: filters input
  (e/server
   (let [!cart (atom #{1})
         cart (e/watch !cart)
         {:keys [pk] :as _type} (get types type-k)]
     (Table DisplayCell
            types
            type-k
            (Search type-k (when (seq cart) [[:not [:in pk cart]]]))
            "Add"
            (e/fn [id] (e/server (do (swap! !cart conj id) nil))))
     (Table DisplayCell
            types
            type-k
            (when (seq cart) (Search type-k [[:in pk cart]]))
            "Remove"
            (e/fn [id] (e/server (do (swap! !cart disj id) nil))))))
  ;; TODO: Submit button (and take callback? return (e/amb) while not selected?)
  )

(e/defn Main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              e/http-request (e/server ring-request)]
      ; mandatory wrapper div https://github.com/hyperfiddle/electric/issues/74
      (dom/div (dom/props {:style {:display "contents"}})
               (e/server
                (YakSearch mock/Search mock/DisplayCell model/types :task))))))
