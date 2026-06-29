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

Services are defined in `resources/services.edn` and started via [modular](https://github.com/pink-gorilla/modular) / juxt.clip.

Configuration:

- `resources/config.edn` — web server port, token/OAuth2, logging
- `resources/users.edn` — local user accounts (hashed passwords)

Open:

- http://localhost:3000 (Hyper frontend)
- http://localhost:9000 (Flowy OMS socket server)

The demo also starts a seeded Datahike trade DB, OMS server, and test-order poller (same stack as quanta-blotter/demo `:cli-server`). Delete `demo/trade-db-oms-server` to reset the database.

nREPL listens on port **9100** when the app is running.

## Users and passwords

`token` compares login passwords against **pre-hashed** values in `users.edn` (blake2b-128 hex). Passwords are not hashed on each app start.

To add or change a user:

1. Edit `resources/users.edn` with plain-text `:password` values.
2. Run `clojure -X:hash-users` once to write hashes back to the file.
3. Start the app with `clojure -X:run`.

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

Demo `:run` uses a seeded trade DB and OMS for `/backoffice` and `/accounts`. The antman sim pages still use in-memory `positions*` and `trades*` atoms with Hyper watches for live HTML updates.
