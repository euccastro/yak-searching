(ns electric-starter-app.ui
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]))

(e/defn On [evt Cb & args]
  (e/client
   (when-some [t (let [e (dom/On evt identity nil)
                       [t _] (e/Token e)]
                   t)]
     (case (e/apply Cb args)
       (do (t) (e/amb))))))

#?(:cljs (defn own [event] (.stopPropagation event) (.preventDefault event)))
