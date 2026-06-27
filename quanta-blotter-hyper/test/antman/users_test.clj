(ns antman.users-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [antman.users :as users]
   [token.identity.local :as local-identity]))

(deftest hash-passwords-test
  (testing "hashes plain text and leaves existing hashes unchanged"
    (let [plain {:demo {:password "secret" :roles #{:trader}}}
          hashed (:demo (users/hash-passwords plain))
          again (:demo (users/hash-passwords {:demo hashed}))]
      (is (= (:password hashed) (local-identity/pwd-hash "secret")))
      (is (= hashed again)))))
