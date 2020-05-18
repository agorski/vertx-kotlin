package ago.kotrx.weather

import ago.kotrx.ResponseMaker
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.json.JsonArray
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
    ResponseMaker.sendResponse(
      routingContext,
      weatherClient.weatherForCity(
        routingContext.request().getParam("city")
      )
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
        .rxSend().subscribe(
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

  fun weatherForCity(city: String): Single<JsonObject> {
    return webClient[port, url, "${queryLocation}$city"]
      .timeout(timeout)
      .rxSend()
      .map { obj: HttpResponse<Buffer?> ->
        obj.bodyAsJsonArray()
      }
      .map { j: JsonArray ->
        j.getJsonObject(0).getInteger("woeid")
      }
      .flatMap { woeid: Int ->
        webClient[port, url, "${queryWeather}$woeid"]
          .timeout(timeout)
          .rxSend()
      }
      .map { r: HttpResponse<Buffer> ->
        r.bodyAsJsonObject()
      }.subscribeOn(Schedulers.io())
  }
}
