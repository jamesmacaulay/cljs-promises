(ns examples.github-search
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-promises.core :as p]
            [cljs-promises.async :refer-macros [<?]]
            [goog.dom :as dom]
            [goog.events :as events]
            [cljs.core.async :refer [put! chan <!]])
  (:import [goog.net Jsonp]
           [goog Uri]))

(defn jsonp
  "Performs a JSONP request to the given URL and returns a promise of the
  response."
  [url]
  (p/promise (fn [resolve _]
               (let [req (Jsonp. (Uri. url))]
                 (.send req nil resolve)))))

(defn github-get
  "Performs a JSONP request to the public GitHub API at the given path, and
  returns a promise of the response's data payload as a ClojureScript map
  or vector. The promise will reject with an API-specific error if the response
  status is not 200."
  [path]
  (let [url (str "https://api.github.com" path)]
    (-> (jsonp url)
        (p/then (fn [response]
                  (let [response (js->clj response :keywordize-keys true)]
                    (if (-> response :meta :status (= 200))
                      (:data response)
                      (throw (ex-info (-> response :data :message)
                                      response)))))))))

(defn build-view-context
  "Returns a map of promises for various GitHub resources associated with the
  given user."
  [username]
  {:user (github-get (str "/users/" username))
   :gists (github-get (str "/users/" username "/gists"))
   :repos (github-get (str "/users/" username "/repos?sort=created"))
   :events (github-get (str "/users/" username "/events"))})

(defn link-to [text url]
  (str "<a href=\"" url "\">" text "</a>"))

(defn avatar-view
  "Takes a user map from the GitHub API and returns some HTML for the user's
  avatar."
  [user]
  (-> (str "<img class=\"avatar\" src=\"" (:avatar_url user) "\">")
      (link-to (:html_url user))))

(defn summary-view
  "Takes a user map, a collection of gists, and a collection of repos, and
  returns a little summary of things."
  [user gists repos]
  (let [how-many #(let [n (count %)]
                    (if (>= n 30)
                      "lots of"
                      n))]
    (str (:login user) " has " (how-many gists) " gists and " (how-many repos) " repos.")))

(defn latest-view
  "Returns an HTML list describing the latest gist, repo, and event from the
  arguments."
  [gists repos events]
  (let [[gist repo event] (map first [gists repos events])
        gist-link (-> (or (:description gist)
                          (:html_url gist))
                      (link-to (:html_url gist)))
        repo-link (-> (:name repo)
                                 (link-to (:html_url repo)))
        event-link (if-let [name (-> event :repo :name)]
                     (link-to name (str "https://github.com/" name)))
        event-desc (str (:type event) (when event-link
                                        (str " on " event-link)))]
    (str "<ul>"
         "<li>latest gist: " gist-link "</li>"
         "<li>latest repo: " repo-link "</li>"
         "<li>latest event: " event-desc "</li>"
         "</ul>")))

;; A map of DOM IDs to view description vectors. The first item of each vector
;; is a function which should return HTML or plain text. The second item is a
;; vector of functions (here we're using keywords) which can be applied to a
;; view context map to provide the arguments to the view function.
(def views
  {:github-username-header [:login [:user]]
   :github-avatar [avatar-view [:user]]
   :github-summary [summary-view [:user :gists :repos]]
   :github-latest [latest-view [:gists :repos :events]]})

(defn render!
  "Takes a view map of DOM IDs to view description vectors, and a context
  map whose values may be promises. For each view, its argument-supplying
  functions are applied to the context map. Any promises returned are waited
  on, after which their resolved values are passed to the view function and
  the returned text is set as the HTML of the DOM node identified by the key."
  [view-map context]
  (doseq [[view-id [view-fn args-fns]] view-map]
    (let [node (dom/getElement (name view-id))
          args-promise (p/all ((apply juxt args-fns) context))]
      (go (->> args-promise
               <?
               (apply view-fn)
               (set! (.-innerHTML node)))))))

(defn get-username-from-input []
  (.-value (dom/getElement "github-username")))

;; function copied from http://swannodette.github.io/2013/07/12/communicating-sequential-processes/
(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (put! out e)))
    out))

;; Listen to clicks on the Search button and then render the views based on
;; the username in the input field.
(let [el (dom/getElement "github-output")
      c  (listen (dom/getElement "github-search") "click")]
  (go (while true
        (<! c)
        (->> (get-username-from-input)
             build-view-context
             (render! views)))))
