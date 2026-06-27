(ns antman.sim.generate
  (:import [java.time Instant]
           [java.util UUID]))

(def brokers ["IBKR" "Alpaca" "Bybit" "Binance"])
(def assets ["AAPL" "MSFT" "BTC/USD" "ETH/USD" "SPY" "EUR/USD"])
(def sides [:buy :sell])

(defn- rand-elt [coll]
  (nth coll (rand-int (count coll))))

(defn- rand-price []
  (+ 10.0 (* (rand) 500.0)))

(defn new-id []
  (str (UUID/randomUUID)))

(defn position
  ([] (position {}))
  ([{:keys [id broker asset entry price tp sl]}]
   (let [entry (or entry (rand-price))
         price (or price (+ entry (* (- (rand) 0.5) 5.0)))
         pl (- price entry)]
     {:id (or id (new-id))
      :broker (or broker (rand-elt brokers))
      :asset (or asset (rand-elt assets))
      :entry entry
      :price price
      :pl pl
      :tp (or tp (+ entry 10.0))
      :sl (or sl (- entry 5.0))})))

(defn position-update
  "Update price/pl and set :price-flash to :up or :down when price moves."
  [pos]
  (let [old-price (:price pos)
        price (+ old-price (* (- (rand) 0.5) 2.0))
        pl (- price (:entry pos))
        flash (cond
                (> price old-price) :up
                (< price old-price) :down
                :else nil)]
    (cond-> (assoc pos :price price :pl pl)
      flash (assoc :price-flash flash))))

(defn trade []
  {:id (new-id)
   :broker (rand-elt brokers)
   :asset (rand-elt assets)
   :side (rand-elt sides)
   :qty (+ 1 (rand-int 100))
   :price (rand-price)
   :time (str (Instant/now))})

(defn seed-positions [n]
  (vec (repeatedly n #(position))))
