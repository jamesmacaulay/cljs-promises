(defproject jamesmacaulay/cljs-promises "0.1.0"
  :description "A ClojureScript library for working with JavaScript promises"

  :url "https://github.com/jamesmacaulay/cljs-promises"

  :scm {:name "git"
        :url "https://github.com/jamesmacaulay/cljs-promises"}

  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}

  :dependencies [[org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "examples"
              :source-paths ["src" "examples/src"]
              :compiler {
                :output-to "examples/js/examples.js"
                :output-dir "examples/out"
                :optimizations :none
                :source-map true}}]})
