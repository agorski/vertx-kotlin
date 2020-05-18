package ago.kotrx

import ago.kotrx.ResponseMaker.jsonHeaders
import ago.kotrx.ResponseMaker.ok
import ago.kotrx.ResponseMaker.textHeaders
import ago.kotrx.weather.Weather
import io.vertx.core.Promise
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.healthchecks.HealthCheckHandler
import io.vertx.reactivex.ext.healthchecks.HealthChecks
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.handler.BodyHandler
import org.koin.core.KoinComponent
import org.koin.core.inject
import org.slf4j.LoggerFactory

class ApiVerticle() : AbstractVerticle(), KoinComponent {

  companion object {
    private val logger = LoggerFactory.getLogger(ApiVerticle::class.java)
    val endpointPing = "/ping"
    val endpointHealth = "/health"
  }

  private val weather by inject<Weather>()
  private val healthChecks by inject<HealthChecks>()
  private val config by inject<JsonObject>()

  private fun controller(subRouters: Map<String, Router>): Router {
    val router = Router.router(vertx)

    // add sub routers
    subRouters.forEach { (path, routerForThePath) ->
      router.mountSubRouter(path, routerForThePath)
    }

    // default handler for other requests
    router.route(endpointPing).handler(this::pingEndpoint)

    router.get(endpointHealth).handler(HealthCheckHandler.createWithHealthChecks(healthChecks))

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

  private fun pingEndpoint(routingContext: RoutingContext) {
    ok(routingContext, "", textHeaders)
  }

  private fun defaultEndpoint(routingContext: RoutingContext) {
    ok(routingContext, DefaultResponse(), jsonHeaders)
  }
}

data class DefaultResponse(val text: String = "hello", val time: Long = System.currentTimeMillis())
