(ns freshcode-test.core
  (:require [rum.core :as rum]
            [freshcode-test.data-flow :as data]
            [freshcode-test.component :as components]))

(enable-console-print!)

(data/run-demo)

(rum/mount (components/app data/state)
  (js/document.getElementById "app"))