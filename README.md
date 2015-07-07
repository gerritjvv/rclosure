# rclosure


# Overview

Here I present a pattern for creating and composing "Resource Closures", named so  
because they are functions that close over resources that need to be opened and closed. 
  
Normal function composition does not contemplate resource opening and closing.
 
# What is this trying to solve/improve.

Composition of functions that close over and use resources, providing structure for  
opening and closing resources, and operating on resources in such a way that the closure
itself can be used in a reduce/fold function to accumulate state.

E.g

Composing the functions ```read-from-db``` + ```write-record-to-file``` is not easily composable.  
Normally the logic for ```write-record-to-file``` gets placed inside ```read-from-db``` and  
then if we want to compose ```read-from-db``` + ```send-to-network``` the logic for ```read from-db```
gets duplicated and the ```sent-to-network``` logic placed inside it again.


# Resource Closures and other patterns

**Streams/Sequences/Channels**

Streams and Sequences are the de facto way of handling resources but do not model the resource
lifecycle correctly and normally either leave the resource open (dangling) till the whole stream
has been read, or leaking and closed when the garbarge collector runs.

Channels are a means of handing off messages between different threads or atomic unit, where each channel 
instance handles its own resources and state in a synchronous thread safe manner. They might compose or not
but this is not their design goal, and practically you do not want to create channels between every possible  
pair of functions that handle different resources just for the sake of composition.


**With-* Macros or Callbacks**

In languages that support macros there the usage of ```with-db-connection``` or  
```with-network-conn``` etc is prevalent, but macros do not compose and can not be used  
with the same flexibility as a normal function e.g passed to a reduce/fold function,  
used with map or filter etc. They can also not move state around but rather the resource opened  
at the point where the macro is called must be closed at that point of code also. A function  
can be send to another and resources opened and closed later when required.  

Callbacks do not give you control over opening and closing of resources and are not usually concerned
with maintaining state and do not compose, many times callbacks lead to more frustration than  
anything else.

**Transducers**

Transducers abstract function application away collections, streams etc.  
They are not concerned with opening and closing of resources. 
Transducers and xforms can be used inside of resource clojures.

**Components**

Components are used to model resources (or logic) that live for the life span of the whole
application and can be accessed (ideally also concurrently) from different parts of the  
application logic. They are not concerned with composition, but they are a perfect fit for
using with "Resource Closures".

Components can be used from within "Resource Closures" e.g DB Connection can be retrieved
from a "DB Component" closed over by the Resource Closure and then closed and returned  
to the "DB Component" once the "Resource Closure" is closed.

**Monads**

Monads could be made to work with this but I wanted to use the humble closure.  
Things are easier to understand (my point of view) and functions are prevalent in most languages,
whereas with Monads and Type classes you need super powers :).  


#Resource Closures

Resource closures can be implemented by 3 functions:

1. Factory functions 
2. Resource Closure Generator (or gen) functions
3. Resource Closures

### Factory functions

Resource closures are always returned by a factory function that takes a "Resource Closure Gen" function,
then returns a Resource Closure Gen function.

```
F [f2:ResourceClosureGen] -> ResourceClosureGen
```

### Resource Closure Generator

Resource closure generator functions take an environment, instantiates zero or more resources  
an return an instantiated "Resource Closure" that closes over the resources created.  
 
```
F [env:Any] -> ResourceClosure
```

### Resource Closure

Resource closures are multi arity functions, where each arity represents a particular intent.

```
F2 [] -> InitState   ;; get initial state
F2 [flag-map] -> Any ;; close 
F2 [state x] -> state
```

### Composition

To compose  ```[f1 f2 f3 f4 f5]```  f1 f2 f3 f4 must all be "Resource Closure Factory" functions,  
and f5 must be a "Resource Closure Gen"

Composition is then represented by:

```
run-once [env rcg]
  rc = (rcg env)
  try:
    v = (rc (rc) nil)
  finally:
    (rc empty-map)
  v
  
rcompose 
  [f1 f2]
         (f1 f2)
  [fs:Seq]
         [a b & rs] = (reverse fs)
         rcg = (b a)
         (reduce (fn [rcgN f] (f rcgN)) rcg rs)
```

**Note on the return value**

The return value is the last value returned by the most inner resource closure's ```[state v]``` function.

**Exception Handling**

```run-once``` should always run in a try finally so that even if exceptions are thrown  
the resource closure's close arity is called.

#Examples

Ideas are always better explained with examples.

## Problem Description

Compose reading from a DB, and writing each record to a file.

### Clojure

See: https://github.com/gerritjvv/rclosure/blob/master/clojure/src/rclosure/core.clj


```clojure
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
        (let [[a b & rs] (reverse (cons f1 (cons f2 fs)))
              rcg (b a)]
             ;[f1 f2 f3 f4], reverse [f4 f3 f2 f1]
             ;a = f4, b = f3, rs = [f2 f1], rc-g = (f4 f3), then reduce (fN rc-g)
             (reduce (fn [rcgN f]
                         (f rcgN)) rcg rs))
        (f1 f2)))

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
```

### Scala

### Haskell

### Java Script

### Go

### Ruby

### Rust