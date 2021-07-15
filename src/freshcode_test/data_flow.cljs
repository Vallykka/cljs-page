(ns freshcode-test.data-flow
  (:require [cljs.core.async :as a]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(defonce state (atom {:transaction  {:receiver ""
                                     :count    ""}
                      :currency     "uam"
                      :transactions []
                      :chart-data []
                      :gauge-data []}))
(defonce actions (a/chan))

(defn dispatch
  ([type] (dispatch type nil))
  ([type data]
   (a/put! actions [type data])))

(defmulti apply-action
  (fn [state data dispatch action-type] action-type))

(go-loop []
  (when-let [a (a/<! actions)]
    (let [[type data] a]
      (println "Handle action: " type data)
      (swap! state apply-action data dispatch type))
    (recur)))

(defmethod apply-action :add-transaction
  [state value]
  (if (and (not-any? str/blank? (vals value)) (pos-int? (edn/read-string (get value :count))))
    (let [completed-value (assoc value :date (js/Date.))]
      (update-in state [:transactions] conj completed-value))
    state))

(defmethod apply-action :clear-input
  [state value]
  (assoc state :transaction {:receiver "" :count ""})
  )

(defmethod apply-action :change-receiver
  [state value]
  (assoc-in state [:transaction :receiver] value))

(defmethod apply-action :change-count
  [state value]
  (assoc-in state [:transaction :count] value))

(defmethod apply-action :update-chart
  [state value]
  (let [tr (get state :transactions)
        sum-per-month (apply merge-with + (map #(assoc {}
                                                  (doto
                                                    (js/Date.)
                                                    (.setHours 0 0 0 0)
                                                    (.setMonth (.getMonth (get % :date)))
                                                    (.setYear (.getFullYear (get % :date)))
                                                    )
                                                  (edn/read-string (get % :count)))
                                            tr))
        chart-data (map #(assoc {} :date (first %) :sum (second %)) sum-per-month)]
    (assoc state :chart-data chart-data)))

(defmethod apply-action :update-gauge
  [state value]
  (let [tr (get state :transactions)
        sum-per-receiver (apply merge-with + (map #(assoc {}
                                (get % :receiver)
                                (edn/read-string (get % :count)))
                          (filter #(= (.getMonth (get % :date)) (.getMonth (js/Date.))) tr)))
        gauge-data (map #(assoc {} :x (first %) :y (second %)) sum-per-receiver)]
    (assoc state :gauge-data gauge-data)))




(def demo-data (map (fn [date rec] (assoc {} :receiver rec :date date :count (str (rand-int 200))))
                 (take 23 (cycle
                            (map (fn [m]
                                   (if (< m (inc (.getMonth (js/Date.))))
                                     (doto (js/Date.) (.setMonth m))
                                     (doto (js/Date.) (.setMonth m) (.setYear (dec (.getFullYear (js/Date.)))))))
                                   (range 12))
                            ))
                 (cycle ["Чипсики"
                         "Интернет"
                         "Мировое зло"
                         "Корм для геккона"
                         "Картошка"])))
(defn run-demo []
  (do
    (swap! state assoc-in [:transactions] demo-data)
    (swap! state apply-action nil dispatch :update-chart)
    (swap! state apply-action nil dispatch :update-gauge)
    ))
