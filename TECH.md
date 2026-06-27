# web frontend

- WEB COMPONENTS
  - extend browser html tags with custom components. that's the way to do.
  - need to test all components that shall be used before coding up frontend.
    (grid-table / layout manager / charts) and see that they work for big maximum data;
    most npmjs componets are bloated, and get to performance issues at some point.


- HTMX / REACT / ELECTRIC 

  - HTMX/DATASTAR
    - https://github.com/dynamic-alpha/hyper
    - https://github.com/tatut/ripley
    - https://github.com/andersmurphy/hyperlith

    - https://andersmurphy.com/2025/04/07/clojure-realtime-collaborative-web-apps-without-clojurescript.html
    - https://cells.andersmurphy.com/ 1 billion cell table (datastar demo)

    - https://github.com/starfederation/datastar-clojure/blob/main/README.md
    - https://data-star.dev/
    
    - https://github.com/ashubham/highcharts-webcomponent
    

    - https://github.com/jeremykross/spacegolfbang (missionary in game engine)

  - REACT
    - pitch/uix

  - ELECTRIC
    - is the best solution, but commercial, waiting for termsheet.

  - SCITTLE
    - small fast engine that can be sent code on the fly that creates very fast js code,
    - might be that this is all that is needed.

- ACCESS CONTROL
  - google login
  - emergency code tokens
  - email/password
  - expiry of tokens must happen on saturday?


POSITIONS ORDERS TRADES
- need to be transferred to browser via a websocket protocol that sends
  update instruction 
  - :seq-id 1 :add {:order-id 3 :data data} 
  - :seq-id 2 :update {:order-id 3 :data modified-data}
  - :seq-id 3 :delete {:order-id 3 :data modified-data}
  - trades are :add only (most likely)


