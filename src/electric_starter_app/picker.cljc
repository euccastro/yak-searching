(ns electric-starter-app.picker
  (:require #?(:clj [electric-starter-app.debug :refer [pprint-str]])
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [electric-starter-app.ui :as ui]
            [electric-starter-app.ui :as util]))

#_(e/defn handle-meta-keys [e input-node return !selected V! opt-by-id]
  (case (.-key e)
    "Escape"    (do (ui/own e) (.blur input-node) (return nil))
    "ArrowDown" (do (ui/own e) (swap! !selected next-option))
    "ArrowUp"   (do (ui/own e) (swap! !selected prev-option))
    "Enter"     (do (ui/own e) (when-some [elem @!selected]
                                  (let [id (ui/get-id elem)]
                                    (.blur input-node)
                                    (return (e/server (V! (get opt-by-id id)))))))
    "Tab"       (if-some [elem @!selected]
                  (let [id (ui4/get-id elem)] (return (e/server (V! (get opt-by-id id)))))
                  (return nil))
    #_else      ::unhandled))

#_(e/defn picker
  "Server biased picker."
  [v V! unV! Options OptionKey OptionLabel & body]
  (util/do1
   (e/client
    (dom/div
     (dom/props {:class "hyperfiddle-tag-picker"})
     (let [container-node dom/node]
       (dom/ul
        (dom/props {:class "hyperfiddle-tag-picker-items"})
        (e/server
         (e/for [id v]
           (let [txt (OptionLabel id)]
             (e/client
              (dom/li
               (dom/text txt)
               (dom/span
                (dom/text "×")
                (ui/On "click"
                       (e/fn [ev]
                         (ui/own ev)
                         (e/server
                          (e/call unV! id)))))))))))
       (dom/div
        (dom/props {:class "hyperfiddle-tag-picker-input-container"})
        (let [input-container-node dom/node]
          (dom/input
           (dom/props {:placeholder "Click and type to search"})
           (let [input-node dom/node]
             (binding [dom/node container-node]
               (ui/On "click" (e/fn [ev]
                                (ui/own ev)
                                (ui/focus input-node))))
             (if (e/server (nil? V!))
               (dom/props {:disabled true})
               (let [[state ret]
                     ;; XXX
                     (e/do-event-pending
                      [e (e/listen> dom/node "focus")]
                      (let [return# (m/dfv)
                            search# (dom/On "input" ui/value "")]
                        (binding [dom/node input-container-node#]
                          (let [!selected (atom nil), selected (e/watch !selected)]
                            (dom/div (dom/props {:class "hyperfiddle-modal-backdrop"})
                                     (ui/On "click" (e/fn [] (return nil))))
                            (e/server
                             (let [options (Options search)
                                   ks (e/for [o options#]
                                        (OptionKey o))
                                   opt-by-k (zipmap ks options)]
                               (e/client
                                (ui/On "keydown"
                                       (e/fn [e]
                                         (handle-meta-keys e input-node return !selected V! opt-by-k)))
                                (dom/ul
                                 (e/server
                                  (ui4/for-truncated [id# ks#] (config/option-limit)
                                                     (let [o# (get opt-by-k# id#)]
                                                       (e/client
                                                        (dom/li (dom/text (e/server (new OptionLabel# o#)))
                                                                (swap! !selected# ui4/select-if-first dom/node)
                                                                (e/on-unmount #(swap! !selected# ui4/?pass-on-to-first dom/node))
                                                                (ui4/track-id dom/node id#)
                                                                (ui4/?mark-selected selected#)
                                                                (dom/on "mouseover" (e/fn [e#] (reset! !selected# dom/node)))
                                                                (ui4/return-on-click return# V!# o#))))))))))))
                        (let [ret# (new (e/task->cp return#))]
                          (case ret# (set! (.-value input-node#) ""))
                          ret#)))]
                 (case state# (::e/pending ::e/failed) (throw ret#) nil)))))))
       ~@body)))))
