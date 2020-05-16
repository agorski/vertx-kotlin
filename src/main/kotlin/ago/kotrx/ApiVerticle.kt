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
import org.koin.core.KoinComponent
import org.koin.core.inject

class ApiVerticle() : AbstractVerticle(), KoinComponent {
  private val weather by inject<Weather>()

  private fun controller(subRouters: Map<String, Router>): Router {
    val router = Router.router(vertx)

    // add sub routers
    subRouters.forEach { (path, routerForThePath) ->
      router.mountSubRouter(path, routerForThePath)
    }

    // default handler for other requests
    router.route("/").handler(this::defaultEndpoint)
    router.route().handler(BodyHandler.create())

    return router
  }

  override fun start(startPromise: Promise<Void>) {

    val routers = mapOf(
      Weather.endpoint to weather.routers()
    )
    val options = HttpServerOptions()
      .setLogActivity(true)
    vertx
      .createHttpServer(options)
      .requestHandler(controller(routers))
      .rxListen(8888)
      .ignoreElement()
      .subscribe(startPromise::complete, startPromise::fail)
  }

  private fun defaultEndpoint(routingContext: RoutingContext) {
    ResponseMaker.sendResponse(
      routingContext,
      Single
        .just(routingContext.request().absoluteURI())
        .subscribeOn(Schedulers.io())
        .map {
          DefaultResponse()
        }
    )
  }
}

data class DefaultResponse(val text: String = "hello", val time: Long = System.currentTimeMillis())
