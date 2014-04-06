(ns examples
  (:require [examples.github-search]
            [examples.replicated-search]))

(enable-console-print!)

(cljs-promises.async/extend-promises-as-pair-channels!)
