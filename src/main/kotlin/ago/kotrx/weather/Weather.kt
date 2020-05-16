package ago.kotrx.weather

import ago.kotrx.ResponseMaker
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import java.time.Duration

class Weather(private val vertx: Vertx) {
  companion object {
    const val endpoint = "/weather"
  }

  private val weatherLogic = WeatherClient(vertx)

  fun routers(): Router {
    val router = Router.router(vertx)
    router.get("/:city").handler(this::weatherForCity)
    return router
  }

  private fun weatherForCity(routingContext: RoutingContext) {
    ResponseMaker.sendResponse(
      routingContext,
      weatherLogic.weatherForCity(
        routingContext.request().getParam("city")
      )
    )
  }

}

class WeatherClient(vertx: Vertx) {
  private val webClient = WebClient.create(vertx)

  fun weatherForCity(city: String): Single<JsonObject> {
    return webClient["www.metaweather.com", "/api/location/search/?query=" + city]
      .timeout(Duration.ofSeconds(5).toMillis())
      .rxSend()
      .map { obj: HttpResponse<Buffer?> -> obj.bodyAsJsonArray() }
      .map { j: JsonArray ->
        j.getJsonObject(0).getInteger("woeid")
      }
      .flatMap { woeid: Int ->
        webClient["www.metaweather.com", "/api/location/$woeid"].timeout(
          Duration.ofSeconds(5).toMillis()
        ).rxSend()
      }
      .map { r: HttpResponse<Buffer> ->
        r.bodyAsJsonObject()
      }.subscribeOn(Schedulers.io())
  }
}
