(ns antman.users
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.pprint :as pprint]
   [token.identity.local :as local-identity]))

(defn- password-hashed?
  "Token stores blake2b-128 hex digests (32 lowercase hex chars)."
  [s]
  (and (string? s)
       (= 32 (count s))
       (boolean (re-matches #"[0-9a-f]+" s))))

(defn hash-passwords
  "Hash plain-text :password values; leave already-hashed passwords unchanged."
  [users]
  (into {}
        (map (fn [[id user]]
               [id (update user :password
                           #(if (password-hashed? %) % (local-identity/pwd-hash %)))])
             users)))

(defn- write-edn-file!
  [path data]
  (with-open [w (io/writer path)]
    (binding [*print-length* nil
              *print-level* nil]
      (pprint/pprint data w))))

(defn- users-file
  [path]
  (let [file (io/file path)]
    (when-not (.exists file)
      (throw (ex-info "users.edn not found" {:path (.getAbsolutePath file)})))
    file))

(defn- hash-users-file* [path]
  (let [file (users-file path)
        data (edn/read-string (slurp file))
        users (:users data)]
    (when-not (map? users)
      (throw (ex-info "users.edn must contain a :users map" {:path path :users users})))
    (let [hashed (assoc data :users (hash-passwords users))]
      (write-edn-file! file hashed)
      (println "Hashed passwords written to" (.getPath file))
      hashed)))

(defn hash-users-file!
  "Read users.edn, hash any plain-text passwords, write hashes back to the file.

  Callable via `clojure -X:hash-users` (ignores the exec-args map)."
  ([] (hash-users-file* "resources/users.edn"))
  ([arg]
   (if (string? arg)
     (hash-users-file* arg)
     (hash-users-file* "resources/users.edn"))))
