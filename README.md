Sample reactive vert.x app with kotlin and RxJava2.

# Technology
|  |  |
| --- | --- |
| language | [kotlin](https://kotlinlang.org/) |
| tool-kit | [vert.x](https://vertx.io/) |
| reactive | [RxJava2](https://github.com/ReactiveX/RxJava) |
| DI | [koin](https://github.com/InsertKoinIO/koin/) |

To do:
- config

# Run server

server runs on *port 8888*

```./gradlew run```

# Stress test
run server and then execute:

```ab -k -c 15 -n 10000 127.0.0.1:8888/```

# Request examples

### Default handler
```
http "127.0.0.1:8888/"
```

### Weather for the city
```
http "127.0.0.1:8888/weather/berlin"

http "127.0.0.1:8888/weather/paris"
```

# Self contained jar
build
```
./gradlew shadowJar

```
run
```
java -jar kotrx-1.0.0-SNAPSHOT-fat.jar
```


