(ns cljs-promises.async)

(defmacro <?
  "Expects `expr` to be a channel or ReadPort which produces [value nil] or
  [nil error] pairs, and returns values and throws errors as per `consume-pair`."
  [expr]
  `(cljs-promises.async/consume-pair (cljs.core.async/<! ~expr)))
