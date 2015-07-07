(ns
  ^{:doc "Utility functions for the resource closure pattern"}
  rclosure.api)


(defn run-once
      "Takes an environment and a resource closure gen (or composed gen from rcompose)
       instantiates a resource closure and runs it, the finally closes the resource closure
       the result of the resource closure is returned"
      [env f]
      {:pre [(not (fn? env)) (fn? f)]}
      (let [rc (f env)]
           (try
             (rc (rc) nil)
             (finally (rc {})))))

(defn rcompose
      "Compose all functions in f1, f2 and fs from left to right.
       [f1 f2 f3] becomes (f1 (f2 f3))
       All other functions but the last should be factory functions and return resource closure gens
       which in turn return resource closures. i.e
       rc-g = (f1 fN)
       rc = (rc-g env)
       init = (rc)
       v = (rc init vN)
       (rc) ; close

       The last function should be a resource closure gen
       Returns a resource closure gen"
      [f1 f2 & fs]
      {:pre [(fn? f1) (fn? f2) (or (not fs) (coll? fs))]}
      (if fs
        (let [[a & rs] (reverse fs)]
             ;a is a resource gen
             ;rs are resource factories
             (f1 (f2 (reduce (fn [rcgN f]
                                 (f rcgN)) a rs))))
        (f1 f2)))
