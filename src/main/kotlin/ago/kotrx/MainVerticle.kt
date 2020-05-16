package ago.kotrx

import ago.kotrx.weather.Weather
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.handler.BodyHandler

class MainVerticle() : AbstractVerticle() {
  private val weather = lazy { Weather(vertx) }

  private fun controller(): Router {
    val router = Router.router(vertx)
    router.mountSubRouter("/weather", weather.value.controller())
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
    ResponseMaker.sendResponse(
      routingContext,
      Single
        .just(routingContext.request().absoluteURI())
        .subscribeOn(Schedulers.io())
        .map {
          DefaultResp()
        }
    )
  }
}

data class DefaultResp(val text: String = "hello", val time: Long = System.currentTimeMillis())
