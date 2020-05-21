package ago.kotrx.weather

import ago.kotrx.ResponseMaker
import ago.kotrx.toEither
import arrow.core.Either
import arrow.fx.rx2.ForObservableK
import arrow.fx.rx2.ObservableK
import arrow.fx.rx2.extensions.observablek.monad.monad
import arrow.fx.rx2.fix
import arrow.fx.rx2.k
import arrow.mtl.EitherT
import arrow.mtl.extensions.eithert.monad.monad
import arrow.mtl.value
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
              ResponseMaker.ok(routingContext, it.b, ResponseMaker.jsonHeaders)
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
  fun weatherForCity(city: String): Observable<out Either<Throwable, JsonObject>> {
    return EitherT.monad<Throwable, ForObservableK>(ObservableK.monad()).fx.monad {

      val cityIdByName: Int = !EitherT<Throwable, ForObservableK, Int>(webClient[port, url, "$queryLocation$city"]
        .timeout(timeout)
        .rxSend()
        .map { obj: HttpResponse<Buffer?> ->
          kotlin.runCatching {
            obj.bodyAsJsonArray().getJsonObject(0).getInteger("woeid")
          }.toEither()
        }
        .toObservable().k()
      )
      val weather = !EitherT<Throwable, ForObservableK, JsonObject>(
        webClient[port, url, "$queryWeather$cityIdByName"]
          .timeout(timeout)
          .rxSend()
          .map {
            kotlin.runCatching {
              it.bodyAsJsonObject()
            }.toEither()
          }
          .toObservable().k()
      )

      weather
    }.value().fix().observable
  }

}
