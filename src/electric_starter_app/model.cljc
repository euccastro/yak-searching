(ns electric-starter-app.model)

(def id-width 30)
(def user-name-width 100)

(def types
  {:int {:label "Integer" :pk identity :display identity}
   :string {:label "String" :pk identity :display identity}
   :keyword {:label "Keyword" :pk identity :display identity}
   :user {:label "User"
          :attributes [{:k :id :label "ID" :type :int :width id-width}
                       {:k :name :label "Name" :type :string :width user-name-width}]
          ;; we assume that entries have a single primary key
          :pk :id
          :display :name}
   :task {:label "Task"
          :attributes [{:k :id :label "ID" :type :int :width id-width}
                       {:k :assignee :label "Assignee" :type :user :width user-name-width}
                       {:k :title :label "Title" :type :string :width 300}
                       {:k :creator :label "Creator" :type :user :width user-name-width}
                       {:k :status :label "Status" :type :keyword :width 100}]
          :pk :id
          :display :title}})
