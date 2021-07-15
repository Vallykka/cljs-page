(ns freshcode-test.component
  (:require [cljsjs.victory]
            [rum.core :as rum]
            [clojure.string :as str]
            [cljs-time.core :as time]
            [freshcode-test.data-flow :refer [dispatch]]))

(defn format-to-month
  [date]
  (.toLocaleString date "ru" #js {:month "short"}))

(defn months
  []
  (map (fn [m]
         (if (< m (inc (.getMonth (js/Date.))))
           (doto (js/Date.) (.setMonth m))
           (doto (js/Date.) (.setMonth m) (.setYear (dec (.getFullYear (js/Date.)))))))
    (range 12)))

(defn sort-dates
  [date]
  (let [current-date (js/Date.)
        c-year (.getYear current-date)
        c-month (.getMonth current-date)
        year (.getYear date)
        month (dec (.getMonth date))]
    (if (= c-year year)
      (- month c-month)
      (- year c-year))
    ))

(defn sort-transactions
  [trs]
   (sort (fn [tr]
           (sort-dates (get tr :date)))
     (sort-by #(get % :date) trs))
  )

(rum/defc transactions < rum/reactive
  [state]
  (let [state (rum/react state)]
   [:.transactions {:height 500}
    [:h1 "Транзакции"]
    [:table
     [:thead
      [:tr
       [:th "Месяц"]
       [:th "Получатель"]
       [:th "Сумма"]]]
     [:tbody
      (let [currency (get state :currency)]
        (for [trans (sort-transactions (get state :transactions))]
          [:tr {:key (str trans)}
           [:td (format-to-month (get trans :date))]
           [:td (get trans :receiver)]
           [:td (str/join " " [(get trans :count) currency])]]))]]]))

(rum/defc transaction < rum/reactive
  [state]
  (let [state (rum/react state)]
   [:.transaction
    [:h1 "Добавить транзакцию"]
    [:input {:placeholder "Получатель"
             :value       (get-in state [:transaction :receiver])
             :on-change   #(dispatch :change-receiver (.. % -target -value))}]
    [:input {:placeholder "Сумма"
             :type "number"
             :value       (get-in state [:transaction :count])
             :on-change   #(dispatch :change-count (.. % -target -value))}]
    [:button {:onClick #(do
                          (dispatch :add-transaction (get state :transaction))
                          (dispatch :update-chart)
                          (dispatch :update-gauge)
                          (dispatch :clear-input))}
     "Добавить"]
    ])
  )

(rum/defc chart < rum/reactive
  [state]
  (let [state (rum/react state)
        data (sort-by #(get % :sum) (get state :chart-data))]
    (pr data)
   [:div
    (rum/adapt-class js/Victory.VictoryChart {:width  800
                                              :height 300
                                              :domainPadding {:x 15 :y 5}
                                              :padding { :left 100 :top 50 :bottom 50 :right 50}}
      (rum/adapt-class js/Victory.VictoryAxis {:label      "Месяц"
                                               :tickValues (clj->js (sort #(sort-dates %) (months)))
                                               :tickFormat (fn [date] (format-to-month date))
                                               })
      (rum/adapt-class js/Victory.VictoryAxis {:width           100
                                               :height          10
                                               :dependentAxis   true
                                               :fixLabelOverlap true
                                               :label           "Сумма"
                                               :style           {:axisLabel {:padding 40}}})

       (rum/adapt-class js/Victory.VictoryLine {:y              (fn [datum] (clj->js (str (.-sum datum))))
                                                :x              (fn [datum] (clj->js (.-date datum)))
                                                :labelComponent (rum/adapt-class js/Victory.VictoryLabel {:dy 30})
                                                :labels         (fn [datum] (clj->js (.-sum datum)))
                                                :interpolation  "monotoneX"
                                                :data           (clj->js data)}))]))

(rum/defc gauge < rum/reactive
  [state]
  (let [state (rum/react state)
        data (get state :gauge-data)]
    (if-not (empty? data)
       [:div
        (rum/adapt-class js/Victory.VictoryPie {:width          800
                                                :height         300
                                                :padding        {:top 70 :bottom 50 :right 0 :left 0}
                                                :colorScale     "warm"
                                                :labels         (fn [datum] (str (.-x datum) ": " (.-y datum)))
                                                :style          {:labels {:fontSize 15}}
                                                :data           (clj->js (get state :gauge-data))})])))

(rum/defc app
  [state-atom]
  [:.app-container
   [:.inline
    (transaction state-atom)
    (transactions state-atom)
    ]
   [:.inline
    [:h3.chart "Расходы за год"
     (chart state-atom)]
    [:h3..gauge "Расходы за месяц"
     (gauge state-atom)]
    ]]

)
