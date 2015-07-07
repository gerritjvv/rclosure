(ns rclosure.core-test
  (:require [clojure.test :refer :all]
            [rclosure.example :as rclosure]))


(deftest testrun
         (is (= (rclosure/dummy-main 10) 10)))