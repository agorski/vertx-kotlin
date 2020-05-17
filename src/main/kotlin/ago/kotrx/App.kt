@file:Suppress("UnusedMainParameter")

package ago.kotrx

import ago.kotrx.weather.Weather
import ago.kotrx.weather.WeatherClient
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.config.ConfigRetriever
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.ext.web.client.WebClient
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import org.koin.logger.SLF4JLogger
import org.slf4j.LoggerFactory


fun main(args: Array<String>) {
  val logger = LoggerFactory.getLogger("App")

  val vertx: Vertx = Vertx.vertx()
  val file =
    ConfigStoreOptions()
      .setType("file")
      .setFormat("yaml")
      .setConfig(JsonObject().put("path", "application.yaml"))

  val config: JsonObject =
    ConfigRetriever.create(vertx, ConfigRetrieverOptions().addStore(file)).rxGetConfig().blockingGet()
  logger.debug(Json.encodePrettily(config))

  startKoin {
    logger(SLF4JLogger(Level.INFO))
    // properties( /* properties map */) // declare properties from given map
    // fileProperties() // load properties from koin.properties file or given file name
    environmentProperties() // load properties from environment
    modules(module {
      single { WeatherClient(WebClient.create(vertx), config) }
      single { Weather(get(), vertx) }
      single { config }
    })
  }

  val options = DeploymentOptions()
    .setConfig(config)

  vertx.deployVerticle(ApiVerticle(), options)
}




