(ns electric-starter-app.model)

(def types
  {:int {:label "Integer" :pk identity :display identity}
   :string {:label "String" :pk identity :display identity}
   :keyword {:label "Keyword" :pk identity :display identity}
   :user {:label "User"
          :attributes [{:k :id :label "ID" :type :int}
                       {:k :name :label "Name" :type :string}]
          ;; we assume that entries have a single primary key
          :pk :id
          :display :name}
   :task {:label "Task"
          :attributes [{:k :id :label "ID" :type :int}
                       {:k :assignee :label "Assignee" :type :user}
                       {:k :title :label "Title" :type :string}
                       {:k :creator :label "Creator" :type :user}
                       {:k :status :label "Status" :type :keyword}]
          :pk :id
          :display :title}})
