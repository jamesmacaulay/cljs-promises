# cljs-promises

A ClojureScript library for working with JavaScript promises.

## Requirements

Some functions in this library require that you've already got an ES6 Promise implementation present, either natively or through a [polyfill](https://github.com/jakearchibald/es6-promise). Other functions only require that you're giving them [Promises/A+](http://promises-aplus.github.io/promises-spec/)-compliant objects.

## Overview

This library leverages the power of [core.async](https://github.com/clojure/core.async) to let you write promise code like this:

```clojure
(cljs-promises.async/extend-promises-as-pair-channels!)

(go
 (let [user-promise (get-user "jamesmacaulay")]
   (try
     (println (:blog (<? user-promise)))
     (catch js/Error e
       (println (str "Could't get user data: " (ex-message e))))))
```

The first line globally extends `Promise` instances to act like **read-only channels** which, once resolved, **endlessly produce the same value or error to anyone who takes from them**. Because promises have built-in error semantics which don't have any direct corollary in core.async, there are different ways that we can represent promise results. In this example we use a function which makes it so that values taken from promises are actually `[value error]` pairs, where you get `[value nil]` from a fulfilled promise and `[nil error]` from a rejected promise.

`<?` is a macro provided by `cljs-promises.async` which takes one of these pairs from a promise with core.async's `<!`. If the `error` slot of the pair is non-nil, it throws the error. Otherwise it returns whatever is in the `value` slot.

If you want to customize how a promise's resolution translates to values you get from `<!`, you can use the more general `extend-promises-as-channels!` which lets you provide your own transform functions for fulfilled values and rejected errors.

If you don't want to globally extend promises like this at all (a good idea if you're writing a library or for some other reason don't want to mess with the global scope), the `cljs-promises.async` namespace also provides functions which wrap promises on an ad-hoc basis:

* `cljs-promises.async/pair-port` takes a promise and gives you an object which acts like a read-only channel of `[value error]` pairs as described above.
* `cljs-promises.async/value-port` gives you a `ReadPort` of only resolved values, or `nil` values if a promise is rejected.
* `cljs-promises.async/error-port` is the reverse of `value-port` and gives you just the errors.

## Rationale

We have [CSP](http://en.wikipedia.org/wiki/Communicating_sequential_processes) with core.async, so [why bother with promises](http://swannodette.github.io/2013/08/23/make-no-promises/)? Promises provide very different concurrency semantics compared with CSP channels. While channels provide a powerful abstraction for _passing messages and synchronizing execution_, a promise simply represents a single "eventual value".

This means that HTTP and most filesytem operations are very well suited for promises. Meanwhile, promises are _not_ appropriate for coordinating streams of spontaneous events like key presses or data coming in on a WebSocket. Channels are still the bees' knees when it comes to that kind of thing.

Promises excel at representing single values because they are an **immutable** reference type:

* after a promise is created, the ability to resolve it to a value is not part of its public interface
* a promise only ever resolves to a single fulfilled value or rejected error
* once resolved, a promise will continue to provide its value or error to anyone who asks for it

(Sounds a lot like [futures](http://clojuredocs.org/clojure_core/clojure.core/future) in Clojure, doesn't it?)

Channels in core.async are very different: they are inherently mutable. When you take a value from a channel, it is gone from that channel and no one else will see it. Here's how you might do a JSONP request in core.async:

```clojure
;; jsonp function lifted from http://swannodette.github.io/2013/11/07/clojurescript-101/
(defn jsonp [uri]
  (let [out (chan)
        req (Jsonp. (Uri. uri))]
    (.send req nil (fn [res] (put! out res)))
    out))

(let [uri "http://en.wikipedia.org/w/api.php?action=opensearch&format=json&search=clojure"]
  (go (println (<! (jsonp uri)))))
```

This is fine, but it means that you have to treat the return value of `jsonp` as a single-use wrapper which becomes useless after its value has been taken from it.

If you want to be able to _share_ representations of delayed or partially-delayed values (e.g. nested data structures with some values yet to arrive), then promises are a better fit. Because they are immutable, you can share them between different parts of your code and not have to worry about how they'll be used.

Here's some code which builds a map of promises for different kinds of data about a GitHub user:

```clojure
(defn build-view-context
  [username]
  {:user (github-get (str "/users/" username))
   :gists (github-get (str "/users/" username "/gists"))
   :repos (github-get (str "/users/" username "/repos?sort=created"))
   :events (github-get (str "/users/" username "/events"))})
```

This map can be shared among multiple view functions, and each of those functions can depend on any subset of the included promises. The code which builds a context of eventual values doesn't need to know how those values are going to be used.

I'm sure there are various possibilities available with core.async channels to decouple things in similar ways. There is [mult](http://clojure.github.io/core.async/#clojure.core.async/mult), and [pub](http://clojure.github.io/core.async/#clojure.core.async/pub)/[sub](http://clojure.github.io/core.async/#clojure.core.async/sub), and probably other tools that would help. I would argue, however, that the simplicity of promises makes them a better tool for this particular job.

One of the great things about core.async, though, is that its channels are based on a simple interface composed of a handful of very granular ClojureScript protocols! This means that it's actually really easy to make custom types which act like channels _just enough_ to play nice with the rest of core.async.

## Development

I've been developing cljs-promises with [Light Table](http://www.lighttable.com/), mostly by playing with the examples:

* run `lein cljsbuild auto examples` in the project root to watch the filesystem and compile-on-save
* add a Light Table connection to an external browser and copy the port number from the script tag
* edit `examples/index.html` so that the script tag matches the port
* open up `examples/index.html` in a browser

Then you can eval individual forms from Light Table and save-and-refresh whenever you make big changes.

## Thanks

Special thanks to [David Nolen](http://swannodette.github.io/) for writing so much fantastic code and introductory material for newcomers to ClojureScript and especially core.async. I've adapted or copied some code from his blog in this library (especially the examples), and I've tried to make attribution notes in the code comments wherever appropriate.

## License

This code is released under an MIT license.

Copyright 2014 James MacAulay.
