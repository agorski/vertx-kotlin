Sample reactive vert.x app with kotlin and RxJava2.

# Technology
|  purpose | in use |
| --- | --- |
| language  | [kotlin](https://kotlinlang.org/) |
| reactive  | [RxJava2](https://github.com/ReactiveX/RxJava) |
| DI        | [koin](https://github.com/InsertKoinIO/koin/) |
| functional        | [arrow-kt](https://github.com/arrow-kt/arrow) |
| logging   | [logback](https://github.com/InsertKoinIO/koin/blob/master/koin-projects/docs/reference/koin-core/logging.md) |
| tool-kit  | [vert.x](https://vertx.io/) |
| config    | [vertx-config](https://vertx.io/docs/vertx-config/kotlin/) |

### To do:
- swagger
- (https://piotrminkowski.com/tag/configuration/)

# Run server

server runs on *port 8888*

```./gradlew run```

# Stress test
run server and then execute:

```ab -k -c 15 -n 10000 127.0.0.1:8888/```

# Request examples

| endpoint | name| HTTPie|
| --- | --- | --- |
| ```/```| default | ```http "127.0.0.1:8888/"``` |
| ```/ping```| ping | ```http "127.0.0.1:8888/ping"``` |
| ```/health```| health check | ```http "127.0.0.1:8888/health"``` |
| ```/weather/:city```| weather for the city | ```http "127.0.0.1:8888/weather/berlin"``` |

# Self contained jar
build
```
./gradlew shadowJar

```
run
```
java -jar kotrx-1.0.0-SNAPSHOT-fat.jar
```


