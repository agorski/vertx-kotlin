package ago.kotrx.weather

import ago.kotrx.ResponseMaker
import ago.kotrx.runCatchingEither
import ago.kotrx.toObservable
import ago.kotrx.toObservableK
import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.fx.rx2.ForObservableK
import arrow.fx.rx2.ObservableK
import arrow.fx.rx2.extensions.observablek.monad.monad
import arrow.mtl.EitherT
import arrow.mtl.extensions.eithert.monad.monad
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.Status
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.healthchecks.HealthChecks
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import java.time.Duration

class Weather(private val weatherClient: WeatherClient, private val vertx: Vertx) {
  companion object {
    const val endpoint = "/weather"
  }

  fun routers(): Router {

    val router = Router.router(vertx)
    router.get("/:city").handler(this::weatherForCity)
    return router
  }

  private fun weatherForCity(routingContext: RoutingContext) {
    weatherClient
      .weatherForCity(routingContext.request().getParam("city"))
      .subscribe(
        {
          when (it) {
            is Either.Right -> {
              when (val jsonMaybe = it.b) {
                is Some -> ResponseMaker.ok(routingContext, jsonMaybe.t, ResponseMaker.jsonHeaders)
                is None -> ResponseMaker.notFound(routingContext)
              }
            }
            is Either.Left -> {
              ResponseMaker.internalError(routingContext, it.a)
            }
          }
        },
        { ResponseMaker.internalError(routingContext, it) }
      )
  }
}

class WeatherClient(
  private val webClient: WebClient,
  config: JsonObject,
  private val healthChecks: HealthChecks
) {
  private val configApi = config.getJsonObject("api_weather")
  private val url = configApi.getString("url")
  private val port = configApi.getInteger("port")
  private val timeout = Duration.ofMillis(configApi.getLong("timeoutMs")).toMillis()
  private val queryLocation = configApi.getString("q_location")
  private val queryWeather = configApi.getString("q_weather")
  private val queryHealthCheck = configApi.getString("q_health_check")

  init {
    registerHealthCheck()
  }

  private fun registerHealthCheck() {
    healthChecks.register(
      "WeatherClient"
    ) { future ->
      webClient[port, url, queryHealthCheck]
        .timeout(timeout)
        .rxSend()
        .subscribeOn(Schedulers.io())
        .subscribe(
          { r ->
            if (r.statusCode() < 400) future.complete(Status.OK()) else {
              future.complete(Status.KO(JsonObject().put("invalid status code (expected 2xx)", "${r.statusCode()}")))
            }
          },
          { ex ->
            future.fail(ex)
          }
        )

    }
  }

  @Suppress("USELESS_CAST", "RemoveExplicitTypeArguments")
  fun weatherForCity(city: String): Observable<out Either<Throwable, Option<JsonObject>>> {
    return EitherT.monad<Throwable, ForObservableK>(ObservableK.monad()).fx.monad {

      val cityIdByName = !EitherT<Throwable, ForObservableK, Option<Int>>(
        webClient[port, url, "$queryLocation$city"]
          .timeout(timeout)
          .rxSend()
          .subscribeOn(Schedulers.io())
          .map { obj: HttpResponse<Buffer?> ->
            runCatchingEither {
              val bodyAsJsonArray = obj.bodyAsJsonArray()
              if (bodyAsJsonArray.isEmpty) {
                Option.empty()
              } else {
                val firstCityObject = bodyAsJsonArray.getJsonObject(0)
                if (!firstCityObject.containsKey("woeid")) {
                  throw Exception("city json does not contain key 'woeid'")
                }
                Option.just(firstCityObject.getInteger("woeid"))
              }
            }
          }
          .toObservableK()
      )

      val weatherForTheCity = cityIdByName.fold(
        { Option.empty<JsonObject>() },// no city id by name
        {
          !EitherT<Throwable, ForObservableK, Option<JsonObject>>(
            webClient[port, url, "$queryWeather$it"]
              .timeout(timeout)
              .rxSend()
              .subscribeOn(Schedulers.io())
              .map {
                runCatchingEither {
                  Option.fromNullable(it.bodyAsJsonObject())
                }
              }.toObservableK()
          )
        }
      )

      weatherForTheCity
    }.toObservable()
  }

}
