Sample reactive vert.x app with kotlin and RxJava2.

To do:
1. DI with koin
https://start.insert-koin.io/#/

2.

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

