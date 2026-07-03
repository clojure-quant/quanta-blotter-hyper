# quanta-blotter-hyper [![GitHub Actions status |clojure-quant/quanta-blotter-hyper](https://github.com/clojure-quant/quanta-blotter-hyper/workflows/CI/badge.svg)](https://github.com/clojure-quant/quanta-blotter-hyper/actions?workflow=CI)[![Clojars Project](https://img.shields.io/clojars/v/io.github.clojure-quant/quanta-blotter-hyper.svg)](https://clojars.org/io.github.clojure-quant/quanta-blotter-hyper)

Web Frontend for quanta-blotter

## Requirements

- JDK **21+** (Hyper uses virtual threads)
  check with `java --version` (we have 25)
- Clojure CLI

## Run

```bash
cd demo
clojure -X:run
```
Open:

- http://localhost:3000 (Hyper frontend)
- http://localhost:9000 (Flowy OMS socket server)


Services are defined in `resources/services.edn` and started via [modular](https://github.com/pink-gorilla/modular) / juxt.clip.

Configuration:

- `resources/config.edn` — web server port, token/OAuth2, logging
- `resources/users.edn` — local user accounts (plain or hashed passwords; hashed on db seed)



The demo also starts a seeded Datahike trade DB, OMS server, and test-order poller (same stack as quanta-blotter/demo `:cli-server`). Delete `demo/trade-db-oms-server` to reset the database.

nREPL listens on port **9100** when the app is running.

## Users and passwords

`users.edn` may use plain-text or pre-hashed `:user/password` values. On database seed, plain passwords are hashed (blake2b-128 hex) before they are stored in Datahike; the database always holds hashes.

To add or change a user, edit `resources/users.edn` and delete `demo/trade-db-oms-server` so the db is recreated and users are re-seeded.

## Test

```bash
clojure -M:test
```

## Stack

| Layer | Library |
|-------|---------|
| Web | [Hyper](https://github.com/dynamic-alpha/hyper) |
| Reactive sim | [Missionary](https://github.com/leonoel/missionary) |
| Layout | [Golden Layout](https://golden-layout.github.io/golden-layout/) v2 |
| Services | [modular](https://github.com/pink-gorilla/modular) |


