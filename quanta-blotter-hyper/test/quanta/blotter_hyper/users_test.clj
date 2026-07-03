(ns quanta.blotter-hyper.users-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [quanta.blotter-hyper.users :as users]
   [token.identity.local :as local-identity]))

(deftest hash-passwords-test
  (testing "hashes plain text and leaves existing hashes unchanged"
    (let [plain [{:user/name "demo"
                   :user/password "secret"
                   :user/roles #{:trader}}]
          hashed (first (users/hash-passwords plain))
          again (first (users/hash-passwords [hashed]))]
      (is (= (:user/password hashed) (local-identity/pwd-hash "secret")))
      (is (= hashed again)))))
