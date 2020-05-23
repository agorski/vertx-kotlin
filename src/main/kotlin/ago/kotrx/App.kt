@file:Suppress("UnusedMainParameter")

package ago.kotrx

import ago.kotrx.ConfigSupport.configRetrieverOptions
import ago.kotrx.weather.Weather
import ago.kotrx.weather.WeatherClient
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.Status
import io.vertx.kotlin.ext.web.client.webClientOptionsOf
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.healthchecks.HealthChecks
import io.vertx.reactivex.ext.web.client.WebClient
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {
  val logger = LoggerFactory.getLogger("App")

  val vertx: Vertx = Vertx.vertx()

  val config: JsonObject =
    ConfigRetriever.create(vertx, configRetrieverOptions).rxGetConfig().blockingGet()
  logger.debug(Json.encodePrettily(config))

  val healthChecks = HealthChecks.create(vertx)
  healthChecks.register("app") { future -> future.complete(Status.OK()) }
  val webClientOptionsOf = webClientOptionsOf(logActivity = false, userAgentEnabled = false)
  startKoin {
    logger(SLF4JLogger(Level.INFO))
    // properties( /* properties map */) // declare properties from given map
    // fileProperties() // load properties from koin.properties file or given file name
    environmentProperties() // load properties from environment
    modules(module {
      single { WeatherClient(WebClient.create(vertx, webClientOptionsOf), config, healthChecks) }
      single { Weather(get(), vertx) }
      single { healthChecks }
      single { config }
    })
  }

  val options = DeploymentOptions()
    .setConfig(config)

  vertx
    .rxDeployVerticle(ApiVerticle(), options)
    .subscribe(
      {},
      { e -> logger.error("api verticle failed to start: ${e.message}", e) })
  //Observable.zip(apiVerticle) when more verticles, wait for all, close in case of error
}




