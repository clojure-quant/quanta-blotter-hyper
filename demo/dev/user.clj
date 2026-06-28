(ns user
  (:require
   [clj-reload.core :as reload]
   [demo.routes :refer [rebuild2!]]
   [modular.system :refer [restart!]]
   ))

#_(reload/init
 {:dirs ["src" "test" "dev" "../quanta-blotter-hyper/src"]
  :no-reload '#{user}})

(defn reload []
  (reload/reload)
  (rebuild2!))

(defn restart []
  (restart!))


(comment
 
 (reload)
  
  
 
 ;
 )
