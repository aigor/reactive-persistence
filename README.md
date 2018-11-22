# ReactivePersistence

Reactive Persistence (R2DBC)

## Idea

UI with:

* status bar (threads, requests)
* some useful operations

## Application Logic


```
                 |= External WebService
UI = WebService =|
                 |= Database
```


## Open questions

* WebFlux Netty does not support HTTP/2, only Tomcat
* Do we need TLS for HTTP/2
* May we somehow interact with the audience?

## The Flow of Examples

* Blocking Web App (Spring WebMVC) [may skip]
* Rx Web App (Spring WebFlux) [+]
* Rx Web App with Blocking External Request (Http 11 Client) [+]
* Rx Web App with Rx External Request (WebClient) [+]
* Rx Web App with Rx External Request and Reactive DB
  * Cassandra
  * MongoDB
  * Couchbase [should skip]
* Rx Web App with Rx External Request and Postgres
  * JDBC wrapped in Thread Pool
  * ADBA
  * R2DBC
* Naked R2DBC example
