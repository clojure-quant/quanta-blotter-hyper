(ns quanta.blotter-hyper.middleware)

(defn wrap-ctx
  [handler ctx]
  (fn
    ([request]
     (let [request (assoc request :hyper/env ctx)]
       (handler request)))
    ([request respond raise]
     (let [request (assoc request :hyper/env ctx)]
       (handler request respond raise)))))