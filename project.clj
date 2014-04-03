(defproject cljs-promises "0.1.0-SNAPSHOT"
  :description "A ClojureScript library for using JS promises with core.async"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojurescript "0.0-2156"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :plugins [[lein-cljsbuild "1.0.2"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["src" "dev"]
              :compiler {
                :output-to "dev.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
