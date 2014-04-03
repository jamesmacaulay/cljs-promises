(ns cljs-promises.dev
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-promises.core :as promises :refer [promise resolve]]
            [cljs-promises.async :as pa :refer-macros [<?]]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :as a :refer [put! take! chan <! >!]]
            [cljs.core.async.impl.protocols :as impl]
            [cljs.core.async.impl.channels :refer [box]]
            [cljs.core.async.impl.dispatch :as dispatch])
  (:import [goog.net Jsonp]
           [goog Uri]))

(enable-console-print!)

(pa/extend-promises-as-pair-channels!)

(def wiki-search-url
  "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=")

(defn jsonp [uri]
  (promise (fn [resolve _]
             (.send (Jsonp. (Uri. uri))
                    nil
                    resolve))))

(defn query-url [q]
  (str wiki-search-url q))


(defn user-query []
  (.-value (dom/getElement "query")))

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (put! out e)))
    out))

(defn init! []
  (let [clicks (listen (dom/getElement "search") "click")
        results-view (dom/getElement "results")]
    (go (while true
          (<! clicks)
          (let [[_ results] (<? (jsonp (query-url (user-query))))]
            (set! (.-innerHTML results-view) (render-query results)))))))

(defn render-query [results]
  (str
    "<ul>"
    (apply str
      (for [result results]
        (str "<li>" result "</li>")))
    "</ul>"))


(init!)
