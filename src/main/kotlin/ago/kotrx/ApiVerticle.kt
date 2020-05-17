package ago.kotrx

import ago.kotrx.weather.Weather
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.handler.BodyHandler
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.LoggerFactory

class ApiVerticle() : AbstractVerticle(), KoinComponent {

  companion object {
    private val logger = LoggerFactory.getLogger(ApiVerticle::class.java)
  }

  private val weather by inject<Weather>()
  private val config by inject<JsonObject>()

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
    val port = config.getInteger("port")
    vertx
      .createHttpServer(options)
      .requestHandler(controller(routers))
      .rxListen(port)
      .ignoreElement()
      .subscribe(
        {
          startPromise.complete()
          logger.info("web server started on port $port")
        },
        startPromise::fail
      )
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
