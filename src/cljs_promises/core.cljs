(ns cljs-promises.core)

(defn promise
  [resolver]
  (js/Promise. resolver))

(defn resolve
  [x]
  (.resolve js/Promise x))

(defn reject
  [x]
  (.reject js/Promise x))


