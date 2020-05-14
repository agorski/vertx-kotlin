package ago.kotrx

import io.reactivex.Single
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonArray
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import io.vertx.reactivex.ext.web.handler.BodyHandler

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
        .map { i: String -> i + " " + System.currentTimeMillis() }
    )
  }

  private fun handleWeather(routingContext: RoutingContext) {
    val client = WebClient.create(vertx)
    sendResponse(
      routingContext,
      client["www.metaweather.com", "/api/location/search/?query=" + routingContext.request().getParam("city")]
        .rxSend()
        .map { obj: HttpResponse<Buffer?> -> obj.bodyAsJsonArray() }
        .map { j: JsonArray ->
          j.getJsonObject(0).getInteger("woeid")
        }
        .flatMap { woeid: Int -> client["www.metaweather.com", "/api/location/$woeid"].rxSend() }
        .map { r: HttpResponse<Buffer> ->
          r.body().toString()
        }
    )
  }

  private fun <T> sendResponse(
    context: RoutingContext,
    asyncResult: Single<T>?
  ) {
    asyncResult?.subscribe(
      { r: T -> ok(context, r.toString()) },
      { ex: Throwable -> internalError(context, ex.message) }
    )
      ?: internalError(context, "invalid_status")
  }

  private fun ok(context: RoutingContext, content: String?) {
    context.response().setStatusCode(200)
      .putHeader("content-type", "application/json")
      .end(content)
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
