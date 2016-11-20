(ns namelos.db
  (:require [clojure.java.jdbc :as sql]))

(def url "postgresql://localhost:5432/playground")

(defn db-url
  ([] (db-url ""))
  ([db-name]
   (str "postgresql://localhost:5432/" db-name)))

;; create and drop db

(defn operate-db [operation]
  (fn [db-name]
    (sql/db-do-commands (db-url) false
                        (str (name operation) " database "
                             (name db-name)))))

(def create-db (operate-db :create))
(def drop-db (operate-db :drop))

;;

(sql/db-do-commands url
                    (sql/create-table-ddl
                     :
                     [[:id :serial "PRIMARY KEY"]
                      [:title :text]]))

(sql/db-do-commands url
                    (sql/create-table-ddl
                     :playground
                     [[:id :serial "PRIMARY KEY"]
                      [:title :text]]))

(sql/query url ["select * from playground"])

(defn insert [data]
  (sql/insert! url :playground data))

(insert {:title "the first todo"})
(insert {:title "the second todo"})
(insert {:title "the third todo"})
