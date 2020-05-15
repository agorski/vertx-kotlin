package ago.kotrx

import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import io.vertx.reactivex.ext.web.handler.BodyHandler
import java.time.Duration

class MainVerticle : AbstractVerticle() {

  private fun controller(): Router {
    val router = Router.router(vertx)
    router.get("/weather/:city").handler(this::handleWeather)
    router.get().handler(this::handleIt)
    router.route().handler(BodyHandler.create())

    return router
  }

  override fun start(startPromise: Promise<Void>) {
    val options = HttpServerOptions()
      .setLogActivity(true)
    vertx
      .createHttpServer(options)
      .requestHandler(controller())
      .rxListen(8888)
      .ignoreElement()
      .subscribe(startPromise::complete, startPromise::fail)

  }

  private fun handleIt(routingContext: RoutingContext) {
    sendResponse(
      routingContext,
      Single
        .just(routingContext.request().absoluteURI())
        .subscribeOn(Schedulers.io())
        .map {
          Resp()
        }
    )
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
    sendResponse(
      routingContext,
      asyncResult
    )
  }

  private fun <T> sendResponse(
    context: RoutingContext,
    asyncResult: Single<T>?
  ) {
    asyncResult?.subscribe(
      { r: T -> ok(context, r) },
      { ex: Throwable -> internalError(context, ex.message) }
    )
      ?: internalError(context, "invalid_status")
  }

  private fun <T> ok(context: RoutingContext, content: T?) {
    if (content != null) {
      context.response().setStatusCode(200)
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(content!!))

    } else {
      context.response().setStatusCode(404).end()
    }
  }

  private fun internalError(
    context: RoutingContext,
    ex: String?
  ) {
    context.response().setStatusCode(500)
      .putHeader("content-type", "text/plain")
      .end(ex)
  }

}

data class Resp(val text: String = "hello", val time: Long = System.currentTimeMillis())
