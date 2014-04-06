(ns cljs-promises.async)

;; Thanks to David Nolen for the name of this macro! http://swannodette.github.io/2013/08/31/asynchronous-error-handling/
;; ...this version works a bit differently, though:
(defmacro <?
  "Expects `expr` to be a channel or ReadPort which produces [value nil] or
  [nil error] pairs, and returns values and throws errors as per `consume-pair`."
  [expr]
  `(cljs-promises.async/consume-pair (cljs.core.async/<! ~expr)))
