(defproject freshcode_test "0.1.0-SNAPSHOT"
  :description "Test project"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.866"]
                 [org.clojure/core.async "1.3.618"]
                 [rum "0.12.6"]
                 [cljsjs/victory "0.24.2-0"]
                 [com.andrewmcveigh/cljs-time "0.5.2"]]
  :plugins [[lein-cljsbuild "1.1.8"]]
  :source-paths ["src" "resources" "target"]
  :resource-paths ["target" "resources"]
  :clean-targets ^{:protect false} ["resources/public/js/" "target"]

  :cljsbuild {:builds {:dev  {:source-paths ["src"]
                              :resource-paths ["resources"]
                              :compiler     {:optimizations :none
                                             :source-map    true
                                             :main freshcode-test.core}}
                       :prod {:source-paths ["src"]
                              :resource-paths ["resources"]
                              :compiler {:main freshcode-test.core
                                         :output-dir  "resources/public/js/"
                                         :output-to    "resources/public/js/main.js"
                                         :source-map   "resources/public/js/main.js.map"
                                         :optimizations :simple
                                         :infer-externs true
                                         :pretty-print  false}}}}

  :profiles {:dev {:dependencies [[com.bhauman/figwheel-main "0.2.13"]
                                  [com.bhauman/rebel-readline-cljs "0.1.4"]]
                   :resource-paths ["target" "resources/public/js/"]
                   :clean-targets ^{:protect false} ["target" "resources/public/js/"]}
             :prod {}})
