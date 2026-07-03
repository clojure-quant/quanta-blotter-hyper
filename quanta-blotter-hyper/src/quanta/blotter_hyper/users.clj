(ns quanta.blotter-hyper.users
  (:require
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [modular.permission.user.datahike :as user-db]
   [token.identity.local :as local-identity]))

(defn- password-hashed?
  "Token stores blake2b-128 hex digests (32 lowercase hex chars)."
  [s]
  (and (string? s)
       (= 32 (count s))
       (boolean (re-matches #"[0-9a-f]+" s))))

(defn hash-passwords
  "Hash plain-text :user/password values; leave already-hashed passwords unchanged."
  [users]
  (mapv (fn [user]
          (update user :user/password
                    #(if (password-hashed? %) % (local-identity/pwd-hash %))))
        users))

(defn- slurp-edn-path
  "Load EDN from classpath (resources/) or filesystem path."
  [path]
  (if-let [resource (io/resource path)]
    (slurp resource)
    (let [file (io/file path)]
      (when-not (.exists file)
        (throw (ex-info "users.edn not found"
                        {:path path
                         :absolute (.getAbsolutePath file)})))
      (slurp file))))

(defn load-edn-users
  [path]
  (let [data (edn/read-string (slurp-edn-path path))
        users (:users data)]
    (when-not (sequential? users)
      (throw (ex-info "users.edn must contain a :users vector" {:path path :users users})))
    users))

(defn seed-edn-users
  "Returns a db seed-fn that transacts users from an edn file.
   Plain-text passwords in the seed file are hashed before transact."
  [path]
  (user-db/seed-users (hash-passwords (load-edn-users path))))
