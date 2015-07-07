(ns rclosure.example
    (:require
      [clojure.java.io :as io]
      [sjdbc.core :as sjdbc]
      [clojure.string :as string]
      [rclosure.api :refer :all])
    (:import [java.io File BufferedWriter]))


;; load the db driver
(import '(org.hsqldb jdbcDriver))

(defn read-from-db
      "Perform a select on the database and sends each record to the resource closure returned from f
       f = resource closure gen
       returns a resource closure gen that takes environment keys = sql:String, jdbc-url:String, user:String
              note that this environment should have all the keys required by f also"
      [f]
      {:pre [(fn? f)]}
      (fn [{:keys [sql jdbc-url user] :as env}]
          {:pre [(string? sql) (string? jdbc-url) (string? user) (fn? f)]}
          (let [conn (sjdbc/open jdbc-url {})
                f2 (f env)]
               (fn ([])
                   ([_ _]
                     (reduce f2 (f2) (sjdbc/query conn sql)))
                   ([close-map]
                     (f2 close-map)
                     (sjdbc/close conn))))))



(defn write-to-file
      "Resource closure gen.
       Takes an environment with keys file:String/File
       opens the file and returns a resource closure
       the file is closed when the resource closure (f close-map) is called"
      [{:keys [file]}]
      {:pre [(or (string? file)
                 (instance? File file))]}
      (let [^BufferedWriter writer (io/writer file)]

           (fn ([] 0)
               ([n record]
                 (.append writer (str n "," (string/join "," (vals record))))
                 (.newLine writer)
                 (inc n))
               ([close-map]
                 (.close writer)))))



(defn monitor-output
      "Wraps arround the resource closure, and prints out write statements
       f = resource closure gen
       Returns a resource closure gen"
      [f]
      {:pre [(fn? f)]}
      (fn [env]
          (let [f2 (f env)]
               (fn ([] (f2))
                   ([state v]
                     (let [v (f2 state v)]
                          (prn "write " v)
                          v))
                   ([close-map] (f2 close-map))))))

(defn setup-db
      "Perform the database setup by created the test table and inserting some dummy values"
      [jdbc-url user pwd n]
       {:pre [(string? jdbc-url) (string? user) (string? pwd) (number? n)]}
      (with-open [conn (sjdbc/open jdbc-url user pwd {})]
                 (sjdbc/exec conn "DROP TABLE IF EXISTS test")
                 (sjdbc/exec conn "CREATE TABLE IF NOT EXISTS test(id int, name varchar(200))")
                 (dotimes [i n]
                          (sjdbc/exec conn "INSERT INTO test (id, name) VALUES(?, ?)" i "abc"))
                 jdbc-url))

(defn dummy-main [n]
      (run-once

        {:sql      "select * from test"
         :jdbc-url (setup-db "jdbc:hsqldb:mem:mymemdb" "SA" "" n)
         :user     "SA"
         :pwd      ""
         :file     "/tmp/myfile.txt"}

        (rcompose read-from-db
                  monitor-output
                  write-to-file)))