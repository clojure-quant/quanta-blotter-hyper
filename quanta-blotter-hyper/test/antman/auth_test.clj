(ns antman.auth-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [antman.auth :as auth]))

(deftest role-authorized-test
  (testing "separate roles — user must hold one of the required roles"
    (is (auth/role-authorized? #{:trader} #{:trader}))
    (is (auth/role-authorized? #{:viewer} #{:viewer :trader}))
    (is (not (auth/role-authorized? #{:viewer} #{:trader})))
    (is (not (auth/role-authorized? #{:admin} #{:trader})))
    (is (not (auth/role-authorized? #{:trader} #{:admin})))))

(deftest normalize-roles-test
  (testing "JWT roles normalize to keyword set"
    (is (= #{:admin :trader} (auth/normalize-roles ["admin" :trader])))))
