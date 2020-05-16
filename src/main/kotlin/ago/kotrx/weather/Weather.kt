package ago.kotrx.weather

import ago.kotrx.ResponseMaker
import io.reactivex.schedulers.Schedulers
import io.vertx.core.json.JsonArray
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import java.time.Duration

class Weather(private val vertx:Vertx) {

  fun controller(): Router {
    val router = Router.router(vertx)
    router.get("/:city").handler(this::handleWeather)
    return router
  }

  private fun handleWeather(routingContext: RoutingContext) {
    val client = WebClient.create(vertx)
    val asyncResult =
      client["www.metaweather.com", "/api/location/search/?query=" + routingContext.request().getParam("city")]
        .timeout(Duration.ofSeconds(5).toMillis())
        .rxSend()
        .map { obj: HttpResponse<Buffer?> -> obj.bodyAsJsonArray() }
        .map { j: JsonArray ->
          j.getJsonObject(0).getInteger("woeid")
        }
        .flatMap { woeid: Int ->
          client["www.metaweather.com", "/api/location/$woeid"].timeout(
            Duration.ofSeconds(5).toMillis()
          ).rxSend()
        }
        .map { r: HttpResponse<Buffer> ->
          r.bodyAsJsonObject()
        }.subscribeOn(Schedulers.io())
    ResponseMaker.sendResponse(
      routingContext,
      asyncResult
    )
  }


}
