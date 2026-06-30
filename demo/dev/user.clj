(ns user
  (:require
   [clj-reload.core :as reload]
   [modular.system :refer [restart!]]))

#_(reload/init
   {:dirs ["src" "test" "dev" "../quanta-blotter-hyper/src"]
    :no-reload '#{user}})

(defn reload []
  (reload/reload)
  (restart!))

(comment
  (reload)

 ;
  )
