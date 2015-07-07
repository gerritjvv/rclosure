(ns rclosure.core-test
  (:require [clojure.test :refer :all]
            [rclosure.core :as rclosure]))


(deftest testrun
         (is (= (rclosure/dummy-main 10) 10)))