(ns examples.replicated-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-promises.core :as p]
            [cljs-promises.async :refer-macros [<?]]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :as async :refer [put! chan <!]]
            [cljs.core.async.impl.protocols :as impl]
            [cljs.core.async.impl.channels :refer [box]]
            [cljs.core.async.impl.dispatch :as dispatch]))

;; adapted from http://swannodette.github.io/2013/07/12/communicating-sequential-processes/
;; c.f. http://talks.golang.org/2012/concurrency.slide#50

(defn fake-search [kind]
  (fn [query]
    (p/promise (fn [resolve _]
                 (js/setTimeout #(resolve [kind query])
                                (rand-int 100))))))

(def web1 (fake-search :web1))
(def web2 (fake-search :web2))
(def image1 (fake-search :image1))
(def image2 (fake-search :image2))
(def video1 (fake-search :video1))
(def video2 (fake-search :video2))

(defn google [query]
  (let [t (p/timeout 80)
        web-search (p/race [t (web1 query) (web2 query)])
        image-search (p/race [t (image1 query) (image2 query)])
        video-search (p/race [t (video1 query) (video2 query)])]
    (-> (p/all [web-search image-search video-search])
        (p/then (partial filter identity)))))

(defn listen [el type]
  (let [c (chan)]
    (events/listen el type #(put! c %))
    c))

(let [el (dom/getElement "replicated-output")
      c  (listen (dom/getElement "replicated-search") "click")]
  (go (while true
        (<! c)
        (dom/setTextContent el (pr-str (<? (google "clojure")))))))
