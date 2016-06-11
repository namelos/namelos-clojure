(ns namelos.web
  (:require [compojure.core :refer [defroutes GET POST]]
            [compojure.route :as route]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [hiccup.form :as form]
            [hiccup.def :refer [defhtml]]
            [clojure.java.jdbc :as sql])
  (:gen-class))

;;;;;;;;;;;;;;;;;;;
;; db connection ;;
;;;;;;;;;;;;;;;;;;;

(defn get-db-url []
  "Get the database url in current environment."
  (or (System/getenv "DATABASE_URL")
      "postgresql://localhost:5432/namelos"))

;;;;;;;;;;;;;;;;;;;
;; db operations ;;
;;;;;;;;;;;;;;;;;;;

(defn drop-table [url table]
  "Drop the specified table"
  (sql/db-do-commands url
                      (str "drop table " table)))

(defn drop [table]
  (drop-table (get-db-url) table))

(defn drop-namelos []
  (drop "namelos"))

(def url (get-db-url))

(drop "testing")

(sql/db-do-commands url (sql/create-table-ddl :testing
                                              [[:id :serial "PRIMARY KEY"]
                                               [:title :text]
                                               [:content :varchar]
                                               [:created_at :timestamp
                                                "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]]))

(sql/insert! url :testing {:title "my first title"
                           :content "my first content"})

(sql/query url ["select * from testing"])

;;;;;;;;;;;;;;;
;; migration ;;
;;;;;;;;;;;;;;;

(defn migrated? []
  (-> (sql/query (get-db-url)
                 [(str "select count(*) from information_schema.tables "
                       "where table_name='namelos'")])
      first :count pos?))



(defn insert-sample []
  (let [url (get-db-url)])
  (sql/insert! url
               :namelos {:title "my title" :content "my content"}))

(defn insert-article [data]
  (sql/insert! url :namelos data))

(defn migrate []
  (when (not (migrated?))
    (println "Initializing table...")
    (sql/db-do-commands (get-db-url)
                        (sql/create-table-ddl
                         :namelos
                         [[:id :serial "PRIMARY KEY"]
                          [:title :text]
                          [:content :varchar]
                          [:created_at :timestamp
                           "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"]])))
  (insert-sample)
  (insert-sample)
  (insert-sample))

(defn query []
  (sql/query (get-db-url) ["select * from namelos"]))

;;;;;;;;;;;;
;; layout ;;
;;;;;;;;;;;;

(defn layout [title & body]
  (html5
   [:head
    [:title title]]
   [:body
    [:div {:class "header"}
     [:h1 "Namelos"]]
    [:div {:class "content"}
     [:p "this is some content"] body]]))

;;;;;;;;;;;
;; views ;;
;;;;;;;;;;;

(defn blog-list [args]
  (map (fn [{:keys [title content]}]
         [:div
          [:h3 title]
          [:p content]])
       args))

(first (blog-list [{:title "title1" :content "content1"}
            {:title "title2" :content "content2"}
            {:title "title3"}]))

(defn blog-form []
  [:div
   (form/form-to [:post "/new"]
                 (form/label "title" "Title") [:br]
                 (form/text-field "title") [:br]
                 (form/label "content" "Content") [:br]
                 (form/text-field "content")
                 (form/submit-button "submit"))])

;;;;;;;;;;;;
;; server ;;
;;;;;;;;;;;;

(defn get-port []
  "Get the port number you want to listen in current environment."
  (or (System/getenv "PORT")
      3000))

(defroutes routes
  (GET "/" []
    (html (blog-list (query))))
  (GET "/new" [] (html (blog-form)))
  (POST "/new" {params :params}
    (let [article
          {:title (params "title")
           :content (params "content")}]
    (insert-article article)
    (layout "POST."
            [:p (str "you have posted" params)]))))

(def app (wrap-params #'routes))

(defn -main []
  (run-jetty #'app {:port (get-port) :join? false}))

(def server (-main))
(.stop server)

